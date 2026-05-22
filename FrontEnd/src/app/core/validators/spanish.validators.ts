import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Email con TLD obligatorio. `Validators.email` de Angular acepta "a@b" sin
 * dominio de nivel superior; aquí exigimos al menos `xx@yy.zz` con TLD de
 * 2+ caracteres.
 */
export function emailWithTldValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value ?? '').toString().trim();
    if (!v) return null;
    return /^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,}$/.test(v) ? null : { emailTld: true };
  };
}

/**
 * Teléfono español (móvil o fijo): 9 dígitos empezando por 6/7/8/9, con o sin
 * prefijo internacional +34. Acepta espacios y guiones, que se ignoran.
 */
export function spanishPhoneValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value ?? '').toString().replace(/[\s-]/g, '');
    if (!v) return null;
    return /^(?:\+?34)?[6-9]\d{8}$/.test(v) ? null : { phoneFormat: true };
  };
}

/**
 * Valida DNI, NIE o CIF español. Chequea formato y dígito/letra de control.
 *  - DNI: 8 dígitos + letra. Letra = "TRWAGMYFPDXBNJZSQVHLCKE"[n mod 23].
 *  - NIE: X/Y/Z + 7 dígitos + letra. Convertimos el prefijo a 0/1/2.
 *  - CIF: letra organización + 7 dígitos + dígito/letra de control.
 */
export function spanishTaxIdValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value ?? '').toString().toUpperCase().replace(/[\s-]/g, '');
    if (!v) return null;
    if (isValidDni(v) || isValidNie(v) || isValidCif(v)) return null;
    return { taxIdFormat: true };
  };
}

// ===== Helpers =====

const DNI_LETTERS = 'TRWAGMYFPDXBNJZSQVHLCKE';

function isValidDni(value: string): boolean {
  const m = /^(\d{8})([A-Z])$/.exec(value);
  if (!m) return false;
  const num = parseInt(m[1], 10);
  return DNI_LETTERS[num % 23] === m[2];
}

function isValidNie(value: string): boolean {
  const m = /^([XYZ])(\d{7})([A-Z])$/.exec(value);
  if (!m) return false;
  const prefix = { X: '0', Y: '1', Z: '2' }[m[1] as 'X' | 'Y' | 'Z'];
  const num = parseInt(prefix + m[2], 10);
  return DNI_LETTERS[num % 23] === m[3];
}

function isValidCif(value: string): boolean {
  const m = /^([ABCDEFGHJNPQRSUVW])(\d{7})([0-9A-J])$/.exec(value);
  if (!m) return false;
  const [, letter, digits, control] = m;
  let evenSum = 0;
  let oddSum = 0;
  for (let i = 0; i < digits.length; i++) {
    const d = parseInt(digits[i], 10);
    if ((i + 1) % 2 === 0) {
      evenSum += d;
    } else {
      const dbl = d * 2;
      oddSum += dbl > 9 ? dbl - 9 : dbl;
    }
  }
  const unit = (evenSum + oddSum) % 10;
  const controlDigit = unit === 0 ? 0 : 10 - unit;
  const controlLetter = 'JABCDEFGHI'[controlDigit];
  if (/[KPQSNRW]/.test(letter)) return control === controlLetter;
  if (/[ABEH]/.test(letter)) return control === String(controlDigit);
  return control === String(controlDigit) || control === controlLetter;
}
