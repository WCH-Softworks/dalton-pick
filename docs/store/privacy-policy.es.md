# Política de privacidad de DaltonPick

**Fecha de entrada en vigor:** [FECHA DE LANZAMIENTO]
**Desarrollador:** WCH Softworks
**Contacto:** [CORREO DE CONTACTO]
**Código fuente:** https://github.com/WCH-Softworks/dalton-pick

> BORRADOR en español latinoamericano neutro. Hay que completar los campos entre corchetes antes de publicar. Es un texto en lenguaje sencillo escrito para la aplicación tal como está en SPEC v0.4; no es asesoría legal. Pide una revisión si agregas alguna función que cambie lo que la aplicación hace con los datos.

## La versión corta

DaltonPick no recopila, guarda, comparte ni vende datos personales. No tiene el permiso de Internet, así que no puede enviar nada fuera de tu teléfono. No tiene anuncios, analíticas ni cuentas.

## Qué hace DaltonPick con tu pantalla

DaltonPick muestra una lupa sobre otras aplicaciones y te dice el nombre del color que está bajo su mira. Para hacerlo necesita ver los píxeles de tu pantalla.

- Cuando sacas la lupa, la aplicación toma **una captura de pantalla** usando el servicio de accesibilidad de Android.
- La captura se mantiene **solo en la memoria** de tu teléfono. Se usa para ampliar la zona bajo la lupa y para leer un pequeño grupo de píxeles y deducir el nombre del color.
- La captura **nunca se guarda en el almacenamiento, nunca se registra, nunca se comparte y nunca se sube**. Se descarta cuando cierras la lupa.
- DaltonPick no lee el texto de tu pantalla, tus mensajes, tus contraseñas ni lo que escribes. El servicio de accesibilidad está configurado para que no pueda leer el contenido de las ventanas.
- Algunas aplicaciones (las bancarias, algunas de video) bloquean las capturas de pantalla. En ellas la captura sale en blanco y DaltonPick te avisa que no puede ver el contenido.

## El servicio de accesibilidad

DaltonPick usa la API AccessibilityService de Android como herramienta de accesibilidad para personas con deficiencia en la visión de los colores. El servicio se usa únicamente para tomar la captura descrita arriba. No realiza acciones por ti, no toca nada y no vigila lo que haces en otras aplicaciones. Tú lo activas en los ajustes de Android (Ajustes → Accesibilidad) y puedes desactivarlo cuando quieras. Cuando está desactivado, la lupa deja de funcionar.

## Modo cámara

Si eliges el modo cámara, DaltonPick pide el permiso de la cámara en ese momento. Los cuadros de la cámara se procesan en la memoria para nombrar colores y **nunca se guardan ni se envían a ningún lugar**. Puedes usar el resto de la aplicación sin dar nunca acceso a la cámara.

## Otros permisos

- **Mostrar sobre otras aplicaciones:** para dibujar la pestaña del borde y la lupa encima de otras aplicaciones.
- **Notificaciones:** para mostrar una pequeña notificación permanente que te deja mostrar u ocultar la pestaña.
- **Servicio en primer plano:** Android lo exige para una superposición que queda disponible.
- **Iniciar con el sistema (opcional):** para restaurar la pestaña del borde después de reiniciar tu teléfono.

Ninguno de estos permisos le da a DaltonPick una forma de recopilar datos sobre ti.

## Qué se guarda en tu teléfono

Tus preferencias (tamaño de la lupa, zoom, borde, idioma y similares) se guardan localmente en tu teléfono. No contienen información personal. Si tienes activada la copia de seguridad de Android, tu teléfono puede incluir estas preferencias en su copia de seguridad normal a tu cuenta de Google. [CONFIRMAR: la versión M0 permite copias de seguridad; si en la versión final pones `allowBackup="false"`, elimina esta oración.] DaltonPick no las envía a ningún lugar.

## Terceros

- DaltonPick **no contiene SDK de terceros**: ni anuncios, ni analíticas, ni reportes de fallos, ni facturación.
- La pantalla "Acerca de" tiene un enlace "Apoyar en Ko-fi". Al tocarlo, la página de Ko-fi se abre en tu navegador. Desde ese momento estás en el sitio web de Ko-fi, que tiene su propia política de privacidad. DaltonPick no le envía nada a Ko-fi. Apoyar el proyecto no desbloquea nada en la aplicación.

## Niños

DaltonPick no recopila datos personales de nadie, incluidos los niños. No está dirigida a los niños como público específico.

## Tus derechos

Como DaltonPick no recopila ni conserva datos personales, no hay nada que consultar, corregir, exportar ni eliminar. Las leyes de protección de datos, como el RGPD (UE), la LGPD (Brasil) y las leyes similares de Latinoamérica, te dan derechos sobre tus datos personales; si tienes dudas sobre cómo se aplican, escribe al contacto indicado arriba.

## Cambios en esta política

Si una versión futura de DaltonPick cambia la forma en que trata los datos, esta página se actualizará antes de publicar esa versión y cambiará la fecha de entrada en vigor. Las versiones anteriores de esta política están en el historial del repositorio público del proyecto.

## Contacto

[CORREO DE CONTACTO]
