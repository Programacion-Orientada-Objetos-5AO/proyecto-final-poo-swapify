/**
 * Configura el envío del formulario de login para autenticar via fetch y
 * persistir el token en el almacenamiento local.
 */
function inicializarLoginForm() {
  const loginForm = document.getElementById("loginForm");
  if (!loginForm) {
    return;
  }

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(loginForm);
    const username = formData.get("username");
    const password = formData.get("password");

    try {
      const response = await fetch("/web/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          Accept: "application/json",
        },
        credentials: "same-origin",
        body: new URLSearchParams({ username, password }),
      });
      let data = {};
      const isJson = response.headers.get("content-type")?.includes("application/json");
      if (isJson) {
        data = await response.json();
      }

      if (response.ok && data.token) {
        localStorage.setItem("jwtToken", data.token);
        window.location.href = "/web/publicaciones";
      } else {
        mostrarMensajeError(data.error || "Error al iniciar sesión");
      }
    } catch (error) {
      console.error("Error:", error);
      mostrarMensajeError("Error de conexión");
    }
  });
}

/**
 * Limpia el token almacenado en el navegador cuando se envía el formulario de
 * cierre de sesión.
 */
function inicializarLogoutForm() {
  const logoutForm = document.getElementById("logoutForm");
  if (!logoutForm) {
    return;
  }

  logoutForm.addEventListener("submit", () => {
    localStorage.removeItem("jwtToken");
  });
}

/**
 * Presenta un mensaje de error en el contenedor estándar del formulario.
 */
function mostrarMensajeError(mensaje) {
  const msgDiv = document.getElementById("msg");
  if (!msgDiv) {
    return;
  }
  msgDiv.className = "alert alert-danger";
  msgDiv.textContent = mensaje;
  msgDiv.style.display = "block";
}

/**
 * Inicializa los manejadores de autenticación una vez cargado el DOM.
 */
document.addEventListener("DOMContentLoaded", () => {
  inicializarLoginForm();
  inicializarLogoutForm();
});
