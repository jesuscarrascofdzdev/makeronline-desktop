package dev.jefe.makeronline.desktop

/**
 * Todos los textos que ve la persona, en un único sitio.
 * Para traducir la app más adelante, solo habrá que tocar aquí.
 */
object Textos {
    const val TITULO_APP = "MakerOnline Desktop"

    // ------------------------------------------------------------ menú
    const val MENU_SESION = "Sesión"
    const val MENU_SUBIR = "Subir"
    const val MENU_DESCARGAR = "Descargar"

    // ------------------------------------------------------------ sesión
    const val SESION_TITULO = "Sesión"
    const val SESION_EXPLICACION =
        "La app usa tu Google Chrome con un perfil propio. Inicias sesión en MakerOnline una vez " +
            "y se queda guardada en este ordenador; nunca sale de él."
    const val CHROME = "Google Chrome"
    const val CHROME_OK = "Instalado"
    const val CHROME_FALTA = "No lo encuentro. Sin Chrome la app no puede funcionar."
    const val DESCARGAR_CHROME = "Descargar Google Chrome"
    const val URL_CHROME = "https://www.google.com/chrome/"
    const val MAKERONLINE = "MakerOnline"
    const val COMPROBANDO = "Comprobando…"
    const val SIN_COMPROBAR = "Sin comprobar"
    const val SESION_INICIADA = "Sesión iniciada"
    fun sesionIniciadaComo(nombre: String) = "Sesión iniciada como $nombre"
    fun sinSesion(motivo: String?) = "Sin sesión" + (motivo?.let { ": $it" } ?: "")
    const val INICIAR_SESION = "Iniciar sesión"
    const val VOLVER_A_INICIAR = "Volver a iniciar sesión"
    const val COMPROBAR = "Comprobar otra vez"
    const val INSTRUCCIONES_LOGIN =
        "Se ha abierto Chrome en MakerOnline. Pulsa \"Log in\", elige \"Continuar con Google\" y entra " +
            "con tu cuenta. No inicies sesión en el propio Chrome (el botón de perfil de arriba a la " +
            "derecha), solo en la web. Cuando veas tu avatar en MakerOnline, vuelve aquí."
    const val YA_HE_ENTRADO = "Ya he entrado"
    const val CANCELAR = "Cancelar"
    fun carpetaDatos(ruta: String) = "Carpeta de datos: $ruta"

    // ------------------------------------------------------------ tareas (se ven en el menú)
    const val TAREA_COMPROBANDO = "Comprobando la sesión…"
    const val TAREA_INICIANDO_SESION = "Iniciando sesión en Chrome…"
    const val TAREA_SIMULANDO = "Simulando…"
    const val TAREA_DESCARGANDO = "Descargando…"
    const val TAREA_BUSCANDO_USUARIO = "Buscando un usuario…"
    const val TAREA_SUBIENDO = "Subiendo…"

    // ------------------------------------------------------------ descargar
    const val DESCARGAR_TITULO = "Descargar"
    const val DESCARGAR_EXPLICACION =
        "Repasa los usuarios de tu lista y descarga sus modelos nuevos (los que aún no tienen tu " +
            "like). Al terminar cada uno le da like, así no se vuelve a bajar. Entre descarga y " +
            "descarga espera de 10 a 30 segundos."
    fun usuariosTitulo(n: Int) = "Usuarios ($n)"
    const val RECARGAR = "Recargar"
    const val LISTA_NO_EXISTE = "Todavía no hay lista de usuarios. Pulsa Editar para añadir el primero."
    const val LISTA_VACIA = "La lista está vacía. Pulsa Editar para añadir usuarios."
    const val EDITAR = "Editar"
    const val CREADOR = "Creador de la app"
    const val CREADOR_DETALLE =
        "Va el primero al descargar toda la lista: así apoyas la app gratis. Puedes desmarcarlo."
    const val CREADOR_FIJO = "Creador de la app · no se puede quitar; se marca o desmarca en Descargar"
    fun esCreador(nombre: String) = "$nombre (creador de la app)"
    const val TODOS_LOS_USUARIOS = "Todos los de la lista"
    const val SOLO_SELECCIONADO = "Solo el seleccionado"
    const val OPCIONES = "Opciones"
    const val ULTIMOS = "Últimos modelos"
    const val ULTIMOS_AYUDA = "de cada usuario"
    const val TODOS_LOS_MODELOS = "Todos sus modelos (más lento; útil la primera vez)"
    const val PERFIL = "Descargar también el perfil de impresión (.3mf)"
    const val MAXIMO = "Máximo de descargas"
    const val MAXIMO_AYUDA = "vacío = sin límite"
    const val VENTANA_CHROME = "Ventana de Chrome"
    const val MODO_APARTADA = "Fuera de la pantalla (recomendado): aparece sola si hay captcha"
    const val MODO_VISIBLE = "Visible todo el rato"
    const val MODO_SIN_VENTANA = "Sin ventana: si sale un captcha, se para"
    const val SIMULAR = "Simular"
    const val SIMULAR_AYUDA = "Solo lista lo que descargaría; no descarga ni da like."
    const val DESCARGAR = "Descargar"
    const val PARAR = "Parar"
    const val PARANDO = "Parando en cuanto se pueda sin dejar nada a medias…"
    fun usuarioActual(t: String) = "Usuario $t"
    fun modeloActual(t: String) = "Modelo $t"
    fun paginas(t: String) = "Leyendo su lista de modelos: página $t"
    fun esperando(s: Long) = "Esperando $s s antes del siguiente"
    fun pausa(s: Long) = "… esperando $s s antes del siguiente"
    fun captcha(para: String, minutos: Long) =
        "MakerOnline pide un captcha para $para. Resuélvelo en la ventana de Chrome (tienes $minutos minutos)."
    const val MENSAJES = "Mensajes"
    const val SIN_MENSAJES = "Aquí irá apareciendo lo que hace."
    const val RESUMEN_SIMULACION = "Simulación terminada: no se ha descargado nada ni se ha dado ningún like."
    const val RESUMEN_PARADO = "Parado (lo has pedido tú). No se ha dejado nada a medias."
    fun resumen(descargados: Int, soloLike: Int, yaTenian: Int, errores: Int) =
        "$descargados descargados · $soloLike solo faltaba el like · $yaTenian ya tenían like · $errores con error"
    const val ABRIR_DESCARGAS = "Abrir la carpeta de descargas"
    const val VER_ERRORES = "Ver detalle de errores"
    const val ERROR_ULTIMOS = "Pon cuántos de los últimos modelos quieres mirar (un número de 1 en adelante)."
    const val ERROR_MAXIMO = "El máximo de descargas tiene que ser un número de 1 en adelante, o dejarlo vacío."
    const val ERROR_SIN_SELECCION = "Elige un usuario de la lista, o marca \"Todos los de la lista\"."
    const val ERROR_LISTA_VACIA = "No hay usuarios en la lista."
    const val ERROR_LISTA_ROTA = "Arregla usuarios.json antes de descargar toda la lista (el error sale a la izquierda)."

    // ------------------------------------------------------------ editar la lista de usuarios
    const val EDITAR_TITULO = "Editar la lista de usuarios"
    const val EDITAR_EXPLICACION =
        "Para añadir a alguien, pega la URL de su perfil de MakerOnline (lo más fiable) o escribe su @ " +
            "o su nombre visible. Antes de añadirlo se comprueba en MakerOnline que existe, y se guarda " +
            "como \"Nombre · URL de su perfil\", que es lo que mejor funciona al descargar."
    const val CAMPO_AGREGAR = "URL de su perfil, @ o nombre"
    const val AGREGAR = "Añadir"
    const val BUSCANDO_USUARIO = "Buscando en MakerOnline…"
    fun chromeOcupado(tarea: String) = "Chrome está ocupado ($tarea). Podrás añadir cuando termine; quitar sí se puede."
    fun agregado(nombre: String, handle: String) = "✔ Añadido: $nombre" + (if (handle.isBlank()) "" else " ($handle)")
    fun yaEsta(nombre: String, como: String) = "$nombre ya está en la lista (como \"$como\")."
    fun quitado(nombre: String) = "Quitado: $nombre"
    const val SIN_USUARIOS = "Todavía no hay nadie en la lista. Añade el primero arriba."
    const val QUITAR = "Quitar"
    const val QUITAR_PREGUNTA = "¿Quitarlo?"
    const val SI_QUITAR = "Sí, quitar"
    const val NO = "No"
    const val NOTA_COPIA =
        "Los cambios se guardan al momento en usuarios.json. La primera vez se guarda una copia del " +
            "archivo tal como estaba en usuarios.json.bak (los comentarios que tuviera se quedan ahí)."
    const val CERRAR = "Cerrar"

    // ------------------------------------------------------------ subir
    const val SUBIR_TITULO = "Subir"
    const val SUBIR_EXPLICACION =
        "Sube como borrador los modelos de la carpeta de pendientes. Cada modelo va en su carpeta, con los " +
            "nombres que quieras: el modelo (.3mf o .stl), al menos una imagen (.jpg o .png; la portada es la " +
            "que se llama portada, cover o como la carpeta, o si no la primera) y los textos en un .json o un " +
            ".txt. Al terminar, cada carpeta pasa a subidos/ (o a errores/, con una captura, si algo falla)."
    fun pendientesTitulo(n: Int) = "Pendientes ($n)"
    const val ABRIR_PENDIENTES = "Abrir la carpeta"
    const val SOLTAR_AQUI =
        "Arrastra aquí las carpetas de tus modelos, o una carpeta que tenga una carpeta por modelo. " +
            "Se copian a pendientes; las tuyas no se tocan."
    const val SUELTA_AHORA = "Suéltalas aquí"
    const val ANADIR_CARPETAS = "Añadir carpetas…"
    const val ELEGIR_CARPETAS = "Elige las carpetas de los modelos"
    const val COPIANDO = "Copiando a pendientes…"
    fun carpetasAnadidas(nombres: List<String>) = "✔ Añadidas a pendientes: " + nombres.joinToString(", ")
    fun noEsCarpeta(nombre: String) = "✘ $nombre no es una carpeta: arrastra la carpeta del modelo, no sus archivos."
    fun yaEnPendientes(nombre: String) = "✘ $nombre ya está en pendientes."
    fun contienePendientes(nombre: String) = "✘ $nombre contiene la propia carpeta de pendientes: no se copia."
    fun yaExisteEnPendientes(nombre: String) =
        "✘ Ya hay una carpeta $nombre en pendientes: cámbiale el nombre a una de las dos."
    fun demasiadosArchivos(nombre: String, max: Int) =
        "✘ $nombre tiene más de $max archivos: no parece la carpeta de un modelo."
    fun noSePuedeCopiar(nombre: String, motivo: String) = "✘ No he podido copiar $nombre: $motivo"
    const val NADA_EN_PENDIENTES = "No hay nada en pendientes."
    const val LISTA_PARA_SUBIR = "Lista para subir"
    fun todasLasValidas(n: Int) = "Todas las que están bien ($n)"
    const val SOLO_LA_SELECCIONADA = "Solo la seleccionada"
    const val REVISAR = "Revisar cada borrador antes de guardarlo"
    const val REVISAR_AYUDA =
        "Con todo subido, la app espera a que mires el formulario en Chrome y pulses Guardar borrador o Descartar."
    const val SUBIR = "Subir"
    const val SUBIR_AYUDA = "Chrome se abre a la vista mientras sube."
    fun revisarAhora(modelo: String) =
        "Revisa el formulario de $modelo en la ventana de Chrome. Cuando lo hayas mirado, elige aquí:"
    const val GUARDAR_BORRADOR = "Guardar borrador"
    const val DESCARTAR = "Descartar (se queda en pendientes)"
    const val PARANDO_SUBIDA = "Parando: lo que no se haya guardado se queda en pendientes…"
    const val RESUMEN_SUBIDA_PARADA = "Parado (lo has pedido tú). Lo que no se ha guardado sigue en pendientes."
    fun resumenSubida(subidos: Int, conError: Int, invalidos: Int, sinSubir: Int) =
        "$subidos subidos · $conError con error · $invalidos no válidos · $sinSubir siguen en pendientes"
    const val ABRIR_SUBIDOS = "Abrir subidos"
    const val ABRIR_ERRORES = "Abrir errores"
    fun verError(carpeta: String) = "Ver el error de $carpeta"
    const val ERROR_SIN_CARPETA = "Elige una carpeta de la lista, o marca \"Todas las que están bien\"."
    const val ERROR_CARPETA_CON_PROBLEMAS = "La carpeta seleccionada tiene problemas: arréglalos antes de subirla."
    const val ERROR_NADA_PARA_SUBIR = "No hay ninguna carpeta lista para subir en pendientes."

    // ------------------------------------------------------------ errores
    fun errorInesperado(e: Throwable) =
        "Algo ha fallado: " + (e.message?.lineSequence()?.firstOrNull() ?: e.toString())
}
