import { FormBuilder, Validators } from '@angular/forms';

import { ExhibitionRequest, ExhibitionResponse } from '../../../core/models/exhibition.models';

/** Formulario de exposición compartido por el alta y la edición. */
export function buildExhibitionForm(fb: FormBuilder) {
  return fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(5000)]],
    venueName: ['', [Validators.required, Validators.maxLength(200)]],
    venueCity: ['', [Validators.required, Validators.maxLength(150)]],
    venueCountry: ['ES'],
    openingDate: ['', [Validators.required]],
    closingDate: ['', [Validators.required]],
  });
}

export type ExhibitionForm = ReturnType<typeof buildExhibitionForm>;

export function exhibitionFormValue(form: ExhibitionForm): ExhibitionRequest {
  const v = form.getRawValue();
  return {
    title: v.title.trim(),
    description: v.description.trim() || undefined,
    venueName: v.venueName.trim(),
    venueCity: v.venueCity.trim(),
    venueCountry: v.venueCountry,
    openingDate: v.openingDate,
    closingDate: v.closingDate,
  };
}

export function patchExhibitionForm(form: ExhibitionForm, e: ExhibitionResponse): void {
  form.patchValue({
    title: e.title,
    description: e.description ?? '',
    venueName: e.venueName,
    venueCity: e.venueCity,
    venueCountry: e.venueCountry ?? 'ES',
    openingDate: e.openingDate,
    closingDate: e.closingDate,
  });
}
