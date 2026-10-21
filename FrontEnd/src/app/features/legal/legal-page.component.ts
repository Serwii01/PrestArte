import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LEGAL_OWNER, LEGAL_UPDATED, LEGAL_VERSION } from '../../core/config/legal';

type LegalDoc = 'privacidad' | 'aviso-legal' | 'cookies' | 'terminos';

/**
 * Textos legales: política de privacidad (RGPD), aviso legal (LSSI),
 * política de cookies y términos de uso. Ruta /legal/:doc.
 */
@Component({
  selector: 'app-legal-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-background">
      <header class="border-b border-border bg-white">
        <div class="mx-auto max-w-4xl px-4 sm:px-6 h-16 flex items-center justify-between">
          <a routerLink="/" class="font-black text-lg text-text-main">PrestArte</a>
          <nav class="flex gap-4 text-sm">
            <a routerLink="/legal/privacidad" class="text-text-secondary hover:text-primary">Privacidad</a>
            <a routerLink="/legal/terminos" class="text-text-secondary hover:text-primary">Términos</a>
            <a routerLink="/legal/cookies" class="text-text-secondary hover:text-primary">Cookies</a>
            <a routerLink="/legal/aviso-legal" class="text-text-secondary hover:text-primary">Aviso legal</a>
          </nav>
        </div>
      </header>

      <article class="mx-auto max-w-4xl px-4 sm:px-6 py-10 legal">
        @switch (doc) {
          @case ('privacidad') {
            <h1>Política de privacidad</h1>
            <p class="meta">Versión {{ version }} · Última actualización: {{ updated }}</p>

            <h2>1. Responsable del tratamiento</h2>
            <p>{{ o.name }}, con NIF {{ o.taxId }} y domicilio en {{ o.address }}. Contacto: {{ o.email }}.</p>

            <h2>2. Qué datos tratamos</h2>
            <ul>
              <li><strong>Identificación y contacto:</strong> nombre o razón social, email, teléfono, NIF/NIE/CIF y, según el perfil, dirección.</li>
              <li><strong>Documento de verificación</strong> que aportas al registrarte para comprobar tu identidad o la de tu entidad.</li>
              <li><strong>Obras:</strong> datos, fotografías, documentos, valor, ubicación y dirección de recogida, historial de procedencia.</li>
              <li><strong>Préstamos:</strong> solicitudes, mensajes del chat, informes de estado con fotografías, certificados de seguro, permisos de exportación, supervisión y valoraciones.</li>
              <li><strong>Datos técnicos:</strong> la dirección IP de las peticiones de acceso (para frenar ataques) y un identificador de sesión guardado en tu navegador.</li>
            </ul>

            <h2>3. Para qué y con qué base legal</h2>
            <ul>
              <li><strong>Prestar el servicio</strong> (gestionar tu cuenta, tus obras y tus préstamos): ejecución del contrato que aceptas al registrarte (art. 6.1.b RGPD).</li>
              <li><strong>Verificar a los usuarios y prevenir fraudes</strong>, incluida la comprobación de la procedencia de las obras: interés legítimo en que la red sea fiable (art. 6.1.f RGPD).</li>
              <li><strong>Cumplir obligaciones legales</strong> (conservación de documentación contractual, requerimientos de autoridades): art. 6.1.c RGPD.</li>
              <li><strong>Avisos del servicio por email</strong> (hitos de tus préstamos, recordatorios de plazos): ejecución del contrato. No enviamos publicidad.</li>
            </ul>

            <h2>4. Quién puede ver tus datos</h2>
            <ul>
              <li><strong>Las otras partes de cada préstamo</strong>, solo en lo necesario: por defecto el coleccionista aparece como «Colección particular» hasta que acepta una solicitud, el valor exacto y la dirección de recogida son privados y la dirección solo la ve el transportista con un envío en curso.</li>
              <li><strong>La administración de PrestArte</strong>, para verificar cuentas y obras.</li>
              <li><strong>Proveedores</strong> que nos prestan servicios (alojamiento y envío de correo), con contratos de encargo del tratamiento.</li>
              <li><strong>OpenStreetMap (Nominatim)</strong>, solo para situar en el mapa ciudades poco habituales al estimar distancias: desde nuestro servidor enviamos únicamente el nombre de la ciudad, nunca direcciones ni datos tuyos.</li>
              <li><strong>Autoridades</strong>, cuando la ley lo exija.</li>
            </ul>
            <p>PrestArte no carga recursos de terceros: tipografías e iconos se sirven desde nuestros propios servidores.</p>

            <h2>5. Cuánto tiempo los conservamos</h2>
            <p>Mientras tu cuenta esté activa. Si te das de baja, anonimizamos tus datos personales y eliminamos tu documento de verificación. Los préstamos, contratos e historiales en los que participaste se conservan durante los plazos legales aplicables (con carácter general, hasta seis años), porque también son datos de las otras partes.</p>

            <h2>6. Tus derechos</h2>
            <p>Puedes ejercer los derechos de acceso, rectificación, supresión, oposición, limitación y portabilidad. Desde <a routerLink="/app/account">Mi cuenta</a> puedes descargar todos tus datos y darte de baja; para lo demás, escríbenos a {{ o.email }}. Si no estás conforme, puedes reclamar ante la Agencia Española de Protección de Datos (aepd.es).</p>

            <h2>7. Seguridad</h2>
            <p>Las contraseñas se guardan cifradas, los documentos sensibles solo son accesibles para las partes autorizadas y el acceso está protegido frente a intentos masivos.</p>
          }

          @case ('aviso-legal') {
            <h1>Aviso legal</h1>
            <p class="meta">Última actualización: {{ updated }}</p>
            <h2>Titular</h2>
            <p>En cumplimiento de la Ley 34/2002 de Servicios de la Sociedad de la Información (LSSI), el titular de {{ o.domain }} es
              {{ o.name }}, con NIF {{ o.taxId }}, domicilio en {{ o.address }} y email {{ o.email }}.
              @if (o.registry) { {{ o.registry }}. }
            </p>
            <h2>Objeto</h2>
            <p>PrestArte es una plataforma que pone en contacto a coleccionistas privados, instituciones culturales y empresas de transporte de arte para gestionar préstamos de obras.</p>
            <h2>Condiciones de uso</h2>
            <p>El uso de la plataforma implica la aceptación de los <a routerLink="/legal/terminos">términos de uso</a>.</p>
            <h2>Propiedad intelectual</h2>
            <p>El diseño y el software de PrestArte pertenecen a su titular. Las imágenes y textos de las obras pertenecen a sus propietarios o autores, que los publican bajo su responsabilidad y autorizan su uso dentro de la plataforma. Los derechos de autor de las obras corresponden a sus artistas o herederos.</p>
            <h2>Responsabilidad</h2>
            <p>PrestArte no se responsabiliza de la información que publican los usuarios ni de los acuerdos que alcanzan entre ellos, en los términos descritos en los términos de uso.</p>
            <h2>Legislación aplicable</h2>
            <p>Este sitio se rige por la legislación española.</p>
          }

          @case ('cookies') {
            <h1>Política de cookies</h1>
            <p class="meta">Última actualización: {{ updated }}</p>
            <p>PrestArte <strong>no utiliza cookies de analítica, publicidad ni seguimiento</strong>.</p>
            <h2>Almacenamiento técnico</h2>
            <p>Cuando inicias sesión, guardamos en el almacenamiento local de tu navegador un identificador de sesión para que no tengas que identificarte en cada página. Es imprescindible para prestar el servicio que pides y por eso no requiere consentimiento (art. 22.2 LSSI). Se borra al cerrar sesión.</p>
            <h2>Servicios de terceros</h2>
            <p>No cargamos contenidos de terceros (tipografías, analítica, vídeos ni redes sociales), así que ningún tercero recibe datos de tu navegación en PrestArte.</p>
            <h2>Cambios</h2>
            <p>Si en el futuro añadimos cookies que requieran tu consentimiento, te lo pediremos antes de instalarlas.</p>
          }

          @case ('terminos') {
            <h1>Términos de uso</h1>
            <p class="meta">Versión {{ version }} · Última actualización: {{ updated }}</p>

            <h2>1. Qué es PrestArte</h2>
            <p>PrestArte es una herramienta para gestionar préstamos de obras de arte entre coleccionistas e instituciones, con la participación de empresas de transporte especializadas. <strong>PrestArte no es parte de los préstamos</strong>: no presta, recibe, transporta ni asegura obras. Los acuerdos se celebran directamente entre los usuarios.</p>

            <h2>2. Cuentas</h2>
            <p>Para operar hay que registrarse y superar una verificación de identidad. Te comprometes a dar datos veraces y a custodiar tu contraseña. Podemos rechazar o suspender cuentas que incumplan estos términos o cuya información no pueda verificarse.</p>

            <h2>3. Obligaciones del coleccionista</h2>
            <ul>
              <li>Ser el propietario legítimo de las obras o estar autorizado para prestarlas, y firmar la declaración de titularidad.</li>
              <li>Describir las obras, su estado y su procedencia de forma veraz.</li>
              <li>Fijar un valor asegurado razonable y revisar con diligencia la documentación que le presenten.</li>
            </ul>

            <h2>4. Obligaciones de la institución</h2>
            <ul>
              <li>Describir con veracidad sus instalaciones y cumplir las condiciones aceptadas para cada obra.</li>
              <li>Contratar un seguro «de clavo a clavo» que cubra el valor acordado y todo el periodo del préstamo.</li>
              <li>Obtener los permisos necesarios (en particular, el de exportación temporal cuando la obra salga de España) y devolver la obra en plazo.</li>
              <li>Respetar la mención en cartela y los usos de imagen autorizados.</li>
            </ul>

            <h2>5. Obligaciones del transportista</h2>
            <p>Prestar el servicio con los medios propios del transporte de obras de arte, documentar el estado de la obra en recogidas y entregas y cumplir los presupuestos aceptados.</p>

            <h2>6. Daños y responsabilidad</h2>
            <p>Los daños, pérdidas o retrasos se rigen por el acuerdo entre las partes y por la póliza de seguro correspondiente. Los informes de estado, el historial y los documentos guardados en PrestArte sirven como prueba entre las partes. PrestArte responde del correcto funcionamiento de la plataforma, pero no de los daños sufridos por las obras ni del incumplimiento de los usuarios.</p>

            <h2>7. Contenidos</h2>
            <p>Al publicar fotografías y documentos nos autorizas a mostrarlos dentro de la plataforma a quien corresponda según la configuración de privacidad de cada obra. Sigues siendo titular de tus contenidos.</p>

            <h2>8. Precio</h2>
            <p>El uso de PrestArte es actualmente gratuito. Si en el futuro se introducen tarifas, se comunicarán con antelación.</p>

            <h2>9. Baja</h2>
            <p>Puedes darte de baja cuando quieras desde Mi cuenta, siempre que no tengas préstamos en curso.</p>

            <h2>10. Cambios y ley aplicable</h2>
            <p>Podemos actualizar estos términos; te avisaremos de los cambios relevantes. Se rigen por la legislación española.</p>
          }

          @default {
            <h1>Página no encontrada</h1>
            <p><a routerLink="/legal/privacidad">Ver la política de privacidad</a></p>
          }
        }
      </article>
    </div>
  `,
  styles: [`
    .legal h1 { font-size: 2rem; font-weight: 900; margin-bottom: .25rem; color: var(--color-text-main, #111); }
    .legal h2 { font-size: 1.125rem; font-weight: 700; margin: 1.75rem 0 .5rem; }
    .legal p, .legal li { font-size: .95rem; line-height: 1.7; }
    .legal ul { list-style: disc; padding-left: 1.25rem; display: grid; gap: .35rem; }
    .legal a { color: var(--color-primary, #2563eb); text-decoration: underline; }
    .legal .meta { color: #6b7280; font-size: .85rem; margin-bottom: 1.5rem; }
  `],
})
export class LegalPageComponent {
  /** Vinculado a `:doc` por withComponentInputBinding(). */
  @Input() doc?: LegalDoc | string;

  protected readonly o = LEGAL_OWNER;
  protected readonly version = LEGAL_VERSION;
  protected readonly updated = LEGAL_UPDATED;
}
