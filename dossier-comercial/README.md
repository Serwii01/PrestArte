# Dosier comercial de PrestArte

Documentos para presentar PrestArte a posibles compradores o socios.

| Documento | Para qué sirve | Cuándo enviarlo |
|---|---|---|
| `PrestArte_One_Pager.pdf` | Resumen de una página: problema, solución, diferencias, estado y contacto. | Primer contacto, por email o LinkedIn. |
| `PrestArte_Presentacion.pptx` / `.pdf` | Presentación de 14 diapositivas con capturas reales y notas del orador. | Reunión o demo. El PDF, para enviar después. |
| `PrestArte_Dossier.pdf` | Dosier de 11 páginas: mercado, producto, ventajas, modelo de negocio, hoja de ruta, riesgos y operación. | Cuando hay interés real, junto con el acuerdo de confidencialidad. |
| `PrestArte_Ficha_Tecnica.pdf` | Arquitectura, modelo de datos, API, seguridad, validación y deuda técnica. | Para el equipo técnico del comprador, en la fase de revisión. |
| `capturas/` | Capturas de la aplicación en funcionamiento, con datos de demostración. | Si piden imágenes sueltas. |

## Antes de enviar

Rellena los textos marcados entre corchetes (resaltados en amarillo en los PDF):

- `[email]` y `[teléfono]`: en el one-pager, la última página del dosier y la última diapositiva.
- `[X semanas]`: duración de la transición y el soporte que ofreces.
- `[nombre de dominio, si aplica]`: en la página «La operación» del dosier. Si no hay dominio, borra esa línea.

## Cómo regenerar los documentos

Los PDF se generan a partir de los HTML de `fuentes/`, y la presentación, a partir de un script de Python.

```bash
cd dossier-comercial/fuentes
./generar.sh                      # dosier, ficha técnica y one-pager (necesita Chromium y la fuente Inter)
python3 presentacion.py           # presentación .pptx (necesita python-pptx)
soffice --headless --convert-to pdf ../PrestArte_Presentacion.pptx --outdir ..
```

La presentación también se puede editar directamente en PowerPoint, Keynote o Google Slides.

## De dónde salen los datos

- **Capturas y verificación**: tomadas el 23 de octubre de 2026 con la aplicación arrancada en local contra MariaDB 10.11, después de recorrer un préstamo completo por la API (de la solicitud a la devolución).
- **Cifras del código**: contadas sobre esta rama (`BackEnd/src` y `FrontEnd/src`).
- **Mercado**: Art Basel y UBS, *The Art Market* (ediciones 2025 y 2026), y Ministerio de Cultura, Estadística de Museos y Colecciones Museográficas. Conviene actualizar las cifras de España con la edición más reciente antes de una presentación formal.
- **Modelo de negocio**: son propuestas; PrestArte todavía no factura.
