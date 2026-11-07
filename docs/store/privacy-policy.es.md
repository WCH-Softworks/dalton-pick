# Política de privacidad de DaltonPick

**Fecha de entrada en vigor:** [FECHA DE LANZAMIENTO]
**Desarrollador:** WCH Softworks
**Contacto:** [CORREO DE CONTACTO]
**Código fuente:** https://github.com/WCH-Softworks/dalton-pick


## La versión corta

DaltonPick no recopila, guarda, comparte ni vende datos personales. No tiene el permiso de Internet, así que no puede enviar nada fuera de tu teléfono. No tiene anuncios, analíticas ni cuentas.

## Qué hace DaltonPick con tu pantalla

DaltonPick muestra una lupa sobre otras aplicaciones y te dice el nombre del color que está bajo su mira. Para hacerlo necesita ver los píxeles de tu pantalla.

- Cuando sacas la lupa, la aplicación toma una captura de pantalla usando el servicio de accesibilidad de Android. Mientras la lupa sigue abierta, toma una nueva cuando la pantalla se desplaza o cambia, para que la lupa siga mostrando lo que realmente hay.
- La captura se mantiene **solo en la memoria** de tu teléfono. Se usa para ampliar la zona bajo la lupa y para leer un pequeño grupo de píxeles y deducir el nombre del color.
- La captura **nunca se guarda en el almacenamiento, nunca se registra, nunca se comparte y nunca se sube**. Se descarta cuando cierras la lupa.
- DaltonPick no lee el texto de tu pantalla, tus mensajes, tus contraseñas ni lo que escribes. El servicio de accesibilidad está configurado para que no pueda leer el contenido de las ventanas.
- Algunas aplicaciones (las bancarias, algunas de video) bloquean las capturas de pantalla. En ellas la captura sale en blanco y DaltonPick te avisa que no puede ver el contenido.

## El servicio de accesibilidad

DaltonPick usa la API AccessibilityService de Android como herramienta de accesibilidad para personas con deficiencia en la visión de los colores. El servicio se usa únicamente para tomar las capturas descritas arriba. Para saber cuándo tomar una nueva, se le avisa *de que* una pantalla se desplazó o cambió, y cuánto se desplazó; no recibe lo que hay en la pantalla. No realiza acciones por ti, no toca nada y no registra lo que haces en otras aplicaciones. Tú lo activas en los ajustes de Android (Ajustes → Accesibilidad) y puedes desactivarlo cuando quieras. Cuando está desactivado, la lupa deja de funcionar.

## Modo grabación de pantalla (alternativo)

Si no quieres activar el servicio de accesibilidad, DaltonPick puede usar la grabación de pantalla de Android. Android te pide permiso cada vez y muestra su propio indicador de grabación mientras funciona. Los cuadros grabados se usan en la memoria, igual que las capturas de arriba, y **nunca se guardan ni se envían a ningún lugar**. Puedes detenerla cuando quieras con el botón Detener de su notificación, desde la aplicación DaltonPick o con los controles de grabación del propio Android.

## Modo cámara

Si eliges el modo cámara, DaltonPick pide el permiso de la cámara en ese momento. Los cuadros de la cámara se procesan en la memoria para nombrar colores y **nunca se guardan ni se envían a ningún lugar**. Puedes usar el resto de la aplicación sin dar nunca acceso a la cámara.

## Otros permisos

- **Mostrar sobre otras aplicaciones:** para dibujar la pestaña del borde y la lupa encima de otras aplicaciones.
- **Notificaciones:** para mostrar una pequeña notificación permanente que te deja mostrar u ocultar la pestaña.
- **Servicio en primer plano (grabación de pantalla):** Android lo exige mientras se usa el modo grabación de pantalla. No se usa con el servicio de accesibilidad.

Ninguno de estos permisos le da a DaltonPick una forma de recopilar datos sobre ti.

## Qué se guarda en tu teléfono

Tus preferencias (tamaño de la lupa, zoom, borde, posición de la pestaña y similares, además de pequeños contadores, como si ya terminaste la introducción) se guardan localmente en tu teléfono. No contienen información personal. Si tienes activada la copia de seguridad de Android, tu teléfono puede incluir estas preferencias en su copia de seguridad normal a tu cuenta de Google. DaltonPick no las envía a ningún lugar.

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
