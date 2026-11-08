export function verificarSesionYAccionar() {
  const usuarioGuardado = localStorage.getItem('usuario');
  
  if (!usuarioGuardado) {
    alert("¡Atención! Debes iniciar sesión para agregar productos al carrito o a tus favoritos.");
    return false;
  }
  
  const usuario = JSON.parse(usuarioGuardado);
  console.log("Acción permitida para el usuario:", usuario.correo);
  
  return usuario;
}

// Usuario guardado en el navegador (o null si no hay sesión)
export function obtenerUsuario() {
  try {
    return JSON.parse(localStorage.getItem('usuario'));
  } catch (e) {
    return null;
  }
}

export function esAdmin() {
  const usuario = obtenerUsuario();
  return !!usuario && usuario.rol === 'ADMIN';
}
