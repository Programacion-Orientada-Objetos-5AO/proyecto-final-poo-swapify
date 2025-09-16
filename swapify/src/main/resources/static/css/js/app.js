// Cliente minimal para usar tu API con JWT (igual que Postman, pero en el navegador)
const API_LOGIN = "/api/auth/login";
const API_PUBLICACIONES = "/api/publicaciones";

function saveToken(token) {
  if (token) localStorage.setItem("jwt", token);
}
function getToken() {
  return localStorage.getItem("jwt");
}
function clearToken() {
  localStorage.removeItem("jwt");
}

function showMsg(text, type = "info") {
  const box = document.getElementById("msg");
  if (!box) return;
  box.className = `alert alert-${type}`;
  box.textContent = text;
  box.style.display = "block";
}

async function apiLogin(username, password) {
  const res = await fetch(API_LOGIN, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  const raw = await res.text();           // soporta string o JSON
  if (!res.ok) {
    let detail = raw;
    try { detail = JSON.parse(raw).detail || raw; } catch {}
    throw new Error(detail || "Error de login");
  }

  // Intentamos detectar token en varios formatos
  let token = raw.trim();
  try {
    const data = JSON.parse(raw);
    token = data.token || data.jwt || data.access_token || token;
  } catch {}
  if (!token || token.length < 10) throw new Error("No se recibió token JWT");
  saveToken(token);
  return token;
}

async function crearPublicacion(dto) {
  const jwt = getToken();
  if (!jwt) throw new Error("Debes iniciar sesión para publicar.");

  const res = await fetch(API_PUBLICACIONES, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${jwt}`,
    },
    body: JSON.stringify(dto),
  });

  const raw = await res.text();
  if (!res.ok) {
    let detail = raw;
    try { detail = JSON.parse(raw).detail || raw; } catch {}
    throw new Error(detail || "Error al crear publicación");
  }
  return raw;
}

// Listeners de formularios (si existen en la página)
document.addEventListener("DOMContentLoaded", () => {
  // LOGIN
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const username = loginForm.username.value.trim();
      const password = loginForm.password.value.trim();
      try {
        await apiLogin(username, password);
        showMsg("Sesión iniciada.", "success");
        setTimeout(() => window.location.href = "/web/publicaciones", 600);
      } catch (err) {
        showMsg(String(err.message || err), "danger");
      }
    });
  }

  // CREAR PUBLICACIÓN
  const pubForm = document.getElementById("pubForm");
  if (pubForm) {
    pubForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const dto = {
        nombre: pubForm.querySelector('[name="nombre"]').value,
        precio: pubForm.querySelector('[name="precio"]').value,
        descripcion: pubForm.querySelector('[name="descripcion"]').value,
        objetoACambiar: pubForm.querySelector('[name="objetoACambiar"]').value,
      };
      try {
        await crearPublicacion(dto);
        showMsg("Publicación creada.", "success");
        setTimeout(() => window.location.href = "/web/publicaciones", 600);
      } catch (err) {
        showMsg(String(err.message || err), "danger");
        if (String(err).includes("Debes iniciar sesión")) {
          setTimeout(() => window.location.href = "/web/login", 1000);
        }
      }
    });
  }

  // LOGOUT (si ponés un botón con id="logoutBtn")
  const btnOut = document.getElementById("logoutBtn");
  if (btnOut) {
    btnOut.addEventListener("click", (e) => {
      e.preventDefault();
      clearToken();
      showMsg("Sesión cerrada.", "success");
      setTimeout(() => window.location.reload(), 500);
    });
  }
});
