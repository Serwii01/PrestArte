import { FormBuilder, Validators } from '@angular/forms';

import { ExhibitionRequest, ExhibitionResponse, ExhibitionStopInput } from '../../../core/models/exhibition.models';

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
    /** Sedes siguientes de una exposición itinerante. */
    stops: fb.nonNullable.array<ReturnType<typeof buildStopGroup>>([]),
  });
}

/** Una sede de la gira. */
export function buildStopGroup(fb: FormBuilder, s?: Partial<ExhibitionStopInput>) {
  return fb.nonNullable.group({
    hostInstitution: [s?.hostInstitution ?? '', [Validators.required, Validators.maxLength(200)]],
    venueName: [s?.venueName ?? '', [Validators.required, Validators.maxLength(200)]],
    venueCity: [s?.venueCity ?? '', [Validators.required, Validators.maxLength(150)]],
    venueCountry: [s?.venueCountry ?? 'ES'],
    openingDate: [s?.openingDate ?? '', [Validators.required]],
    closingDate: [s?.closingDate ?? '', [Validators.required]],
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
    stops: v.stops.map((s) => ({
      hostInstitution: s.hostInstitution.trim(),
      venueName: s.venueName.trim(),
      venueCity: s.venueCity.trim(),
      venueCountry: s.venueCountry,
      openingDate: s.openingDate,
      closingDate: s.closingDate,
    })),
  };
}

export function patchExhibitionForm(form: ExhibitionForm, e: ExhibitionResponse, fb: FormBuilder): void {
  form.patchValue({
    title: e.title,
    description: e.description ?? '',
    venueName: e.venueName,
    venueCity: e.venueCity,
    venueCountry: e.venueCountry ?? 'ES',
    openingDate: e.openingDate,
    closingDate: e.closingDate,
  });
  form.controls.stops.clear();
  (e.stops ?? []).forEach((s) => form.controls.stops.push(buildStopGroup(fb, s)));
}
