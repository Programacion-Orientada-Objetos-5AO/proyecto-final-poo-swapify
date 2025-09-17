// Listeners para logout
document.addEventListener("DOMContentLoaded", () => {
  // LOGOUT
  const btnOut = document.getElementById("logoutBtn");
  if (btnOut) {
    btnOut.addEventListener("click", (e) => {
      e.preventDefault();
      window.location.href = "/logout";
    });
  }
});
