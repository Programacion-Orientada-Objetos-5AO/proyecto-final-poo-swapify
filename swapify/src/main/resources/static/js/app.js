// Listeners para login y logout
document.addEventListener("DOMContentLoaded", () => {
  // LOGIN FORM
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();
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
          // Guardar token en localStorage (el backend setea la cookie)
          localStorage.setItem("jwtToken", data.token);

          // Redirigir a la página principal
          window.location.href = "/web/publicaciones";
        } else {
          // Mostrar error
          const msgDiv = document.getElementById("msg");
          if (msgDiv) {
            msgDiv.className = "alert alert-danger";
            msgDiv.textContent = data.error || "Error al iniciar sesión";
            msgDiv.style.display = "block";
          }
        }
      } catch (error) {
        console.error("Error:", error);
        const msgDiv = document.getElementById("msg");
        if (msgDiv) {
          msgDiv.className = "alert alert-danger";
          msgDiv.textContent = "Error de conexión";
          msgDiv.style.display = "block";
        }
      }
    });
  }

  // LOGOUT
  const logoutForm = document.getElementById("logoutForm");
  if (logoutForm) {
    logoutForm.addEventListener("submit", () => {
      // Limpiar token almacenado localmente antes de enviar el formulario
      localStorage.removeItem("jwtToken");
    });
  }
});
