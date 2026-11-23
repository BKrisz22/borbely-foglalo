import { FormControl, FormGroup, Validators } from '@angular/forms';

import { applyFieldErrors, errorMessage } from './form-errors';

describe('errorMessage', () => {
  it('hiba nélkül üres', () => {
    expect(errorMessage(new FormControl('x'))).toBe('');
  });

  it('a hibakulcshoz tartozó saját üzenetet adja', () => {
    const control = new FormControl('', Validators.required);
    expect(errorMessage(control, { required: 'Kötelező mező.' })).toBe('Kötelező mező.');
  });

  it('a szöveges hibaértéket adja, ha nincs saját üzenet', () => {
    const control = new FormControl('');
    control.setErrors({ server: 'Ezzel az e-mail-címmel már van fiók.' });
    expect(errorMessage(control)).toBe('Ezzel az e-mail-címmel már van fiók.');
  });

  it('különben általános üzenetet ad', () => {
    expect(errorMessage(new FormControl('', Validators.required))).toBe('Hibás érték.');
  });
});

describe('applyFieldErrors', () => {
  it('a szerver mezőhibáit a mezőkre teszi, a többit visszaadja', () => {
    const form = new FormGroup({ email: new FormControl('vilma@example.com') });
    const unmatched = applyFieldErrors(form, {
      email: 'Érvénytelen e-mail-cím.',
      birthDate: 'Ismeretlen mező.',
    });

    expect(form.controls.email.errors).toEqual({ server: 'Érvénytelen e-mail-cím.' });
    expect(form.controls.email.touched).toBe(true);
    expect(unmatched).toEqual(['Ismeretlen mező.']);
  });

  it('a mező módosításakor a szerverhiba eltűnik', () => {
    const form = new FormGroup({ email: new FormControl('a@b.hu') });
    applyFieldErrors(form, { email: 'Foglalt.' });
    form.controls.email.setValue('c@d.hu');
    expect(form.controls.email.errors).toBeNull();
  });
});
