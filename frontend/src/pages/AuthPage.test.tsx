import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { AuthPage } from './AuthPage';

function renderAt(mode: 'login' | 'register') {
  render(
    <MemoryRouter initialEntries={[`/${mode}`]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route path="/" element={<p>Home page</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

const response = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('AuthPage', () => {
  it('shows the message from the API when the login fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(401, { status: 401, message: 'Invalid email or password' }));
    renderAt('login');

    await userEvent.type(screen.getByLabelText('Email'), 'anna@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });

  it('lists every field error of a rejected registration', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(400, {
      status: 400, message: 'Validation failed',
      errors: ['email: Email should be valid.', 'name: Name must be between 3 and 50 characters!'],
    }));
    renderAt('register');

    await userEvent.type(screen.getByLabelText('Name'), 'Anna');
    await userEvent.type(screen.getByLabelText('Email'), 'anna@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'Secret123!');
    await userEvent.click(screen.getByRole('button', { name: 'Sign up' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Validation failed');
    expect(alert).toHaveTextContent('email: Email should be valid.');
  });

  it('logs in, keeps only the refresh token in storage and opens the home page', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(200, {
      id: 1, name: 'Anna', email: 'anna@example.com', role: 'USER',
      accessToken: 'access-token', refreshToken: 'refresh-token',
    }));
    renderAt('login');

    await userEvent.type(screen.getByLabelText('Email'), 'anna@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'Secret123!');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Home page')).toBeInTheDocument();
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string))
      .toEqual({ email: 'anna@example.com', password: 'Secret123!' });
    expect(localStorage.getItem('musify.refreshToken')).toBe('refresh-token');
    expect(JSON.stringify(localStorage)).not.toContain('access-token');
  });
});
