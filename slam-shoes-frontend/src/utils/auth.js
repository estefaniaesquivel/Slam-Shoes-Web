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