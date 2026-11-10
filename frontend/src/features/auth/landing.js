// Página a la que llega cada cuenta al iniciar sesión: la que pidió antes de ingresar
// (?volver=…) o la principal de su rol.
export function landingFor(account, back) {
  // Solo rutas internas: '//sitio' o '/\sitio' llevarían a otro dominio.
  if (back && back.startsWith('/') && !/^\/[/\\]/.test(back) && !back.startsWith('/ingresar')) return back
  const permissions = account?.permissions ?? []
  if (permissions.includes('MANAGE_USERS')) return '/admin'
  if (permissions.includes('VIEW_PRIVATE_COURSE_CONTENT')) return '/cursos'
  return '/mi-cuenta'
}
