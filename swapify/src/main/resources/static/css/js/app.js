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
          },
          body: new URLSearchParams({ username, password }),
        });

        const data = await response.json();

        if (response.ok && data.token) {
          // Guardar token en localStorage y cookie
          localStorage.setItem("jwtToken", data.token);
          document.cookie = `jwtToken=${data.token}; path=/; max-age=86400`; // 1 día

          // Redirigir a la página principal
          window.location.href = "/web/publicaciones";
        } else {
          // Mostrar error
          const msgDiv = document.getElementById("msg");
          msgDiv.className = "alert alert-danger";
          msgDiv.textContent = data.error || "Error al iniciar sesión";
          msgDiv.style.display = "block";
        }
      } catch (error) {
        console.error("Error:", error);
        const msgDiv = document.getElementById("msg");
        msgDiv.className = "alert alert-danger";
        msgDiv.textContent = "Error de conexión";
        msgDiv.style.display = "block";
      }
    });
  }

  // LOGOUT
  const btnOut = document.getElementById("logoutBtn");
  if (btnOut) {
    btnOut.addEventListener("click", (e) => {
      e.preventDefault();
      // Limpiar token
      localStorage.removeItem("jwtToken");
      document.cookie = "jwtToken=; path=/; max-age=0";
      // Redirigir a logout (que puede ser /logout o directamente a /web/)
      window.location.href = "/web/";
    });
  }
});
