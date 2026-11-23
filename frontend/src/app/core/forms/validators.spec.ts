import { FormControl, FormGroup } from '@angular/forms';

import { passwordProblem, sameAs } from './validators';

describe('passwordProblem', () => {
  it('elfogadja a szabályos jelszót, ékezetes betűvel is', () => {
    expect(passwordProblem('Demo1234')).toBeNull();
    expect(passwordProblem('Árvíztűrő1')).toBeNull();
  });

  it('kötelező', () => {
    expect(passwordProblem('')).toBe('A jelszó megadása kötelező.');
    expect(passwordProblem('   ')).toBe('A jelszó megadása kötelező.');
    expect(passwordProblem(null)).toBe('A jelszó megadása kötelező.');
  });

  it('legalább 8 karakter', () => {
    expect(passwordProblem('abc123')).toBe('A jelszó legalább 8 karakter legyen.');
  });

  it('betű és számjegy is kell', () => {
    expect(passwordProblem('abcdefgh')).toBe(
      'A jelszóban legyen legalább egy betű és egy számjegy.',
    );
    expect(passwordProblem('12345678')).toBe(
      'A jelszóban legyen legalább egy betű és egy számjegy.',
    );
  });

  it('legfeljebb 64 karakter és 72 bájt (a BCrypt korlátja)', () => {
    expect(passwordProblem('a1'.repeat(33))).toContain('túl hosszú');
    // 41 karakter, de az „ő” UTF-8-ban 2 bájt: 40 × 2 + 1 = 81 bájt.
    expect(passwordProblem('ő'.repeat(40) + '1')).toContain('túl hosszú');
  });
});

describe('sameAs', () => {
  it('a másik mezővel egyező értéket fogad el', () => {
    const form = new FormGroup({
      password: new FormControl('Demo1234'),
      confirm: new FormControl('Demo1234', sameAs('password', 'Nem egyezik.')),
    });
    expect(form.controls.confirm.errors).toBeNull();

    form.controls.confirm.setValue('Demo12345');
    expect(form.controls.confirm.errors).toEqual({ sameAs: 'Nem egyezik.' });
  });
});
