import { passwordProblem } from '../../core/forms/validators';
import { generateInitialPassword } from './initial-password';

describe('generateInitialPassword', () => {
  it('12 karakteres, és megfelel a jelszószabálynak', () => {
    for (let i = 0; i < 50; i++) {
      const password = generateInitialPassword();
      expect(password).toHaveLength(12);
      expect(passwordProblem(password)).toBeNull();
      expect(password).not.toMatch(/[lIO01]/);
    }
  });

  it('a legrosszabb véletlennél is van benne betű és számjegy', () => {
    const password = generateInitialPassword(() => 0);
    expect(password).toMatch(/\p{L}/u);
    expect(password).toMatch(/\d/);
  });

  it('két hívás (szinte biztosan) különböző jelszót ad', () => {
    expect(generateInitialPassword()).not.toBe(generateInitialPassword());
  });
});
