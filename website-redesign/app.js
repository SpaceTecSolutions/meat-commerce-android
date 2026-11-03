const featuredCuts = [
  { name: "Beef Mignon", image: "assets/beef-mignon.webp", alt: "Beef Mignon from Meat Station", serial: "MS / 001" },
  { name: "Chicken Breast", image: "assets/chicken-breast.webp", alt: "Chicken Breast Sliced from Meat Station", serial: "MS / 002" },
  { name: "Veal Whole Leg", image: "assets/veal-leg.webp", alt: "Veal Whole Leg from Meat Station", serial: "MS / 003" },
];

const hero = document.querySelector(".hero");
const heroFrame = document.querySelector(".hero-image-frame");
const heroImage = document.querySelector("[data-slide-image]");
const heroName = document.querySelector("[data-slide-name]");
const heroCount = document.querySelector("[data-slide-count]");
const progress = document.querySelector("[data-progress]");
const serial = document.querySelector(".image-serial");
let activeCut = 0;
let autoplayTimer;

function showCut(index) {
  if (!heroImage || !heroFrame || !heroName || !heroCount || !progress || !serial) return;
  activeCut = (index + featuredCuts.length) % featuredCuts.length;
  const cut = featuredCuts[activeCut];
  heroFrame.classList.add("is-changing");
  window.setTimeout(() => {
    heroImage.src = cut.image;
    heroImage.alt = cut.alt;
    heroName.textContent = cut.name;
    heroCount.textContent = String(activeCut + 1).padStart(2, "0");
    serial.textContent = cut.serial;
    progress.style.transform = `translateX(${activeCut * 100}%)`;
    heroFrame.classList.remove("is-changing");
  }, 180);
}

function restartAutoplay() {
  window.clearInterval(autoplayTimer);
  if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    autoplayTimer = window.setInterval(() => showCut(activeCut + 1), 6500);
  }
}

document.querySelector("[data-next]")?.addEventListener("click", () => {
  showCut(activeCut + 1);
  restartAutoplay();
});
document.querySelector("[data-previous]")?.addEventListener("click", () => {
  showCut(activeCut - 1);
  restartAutoplay();
});
hero?.addEventListener("mouseenter", () => window.clearInterval(autoplayTimer));
hero?.addEventListener("mouseleave", restartAutoplay);
document.addEventListener("visibilitychange", () => {
  if (document.hidden) window.clearInterval(autoplayTimer);
  else restartAutoplay();
});
restartAutoplay();

const tiltScene = document.querySelector("[data-tilt]");
const tiltCard = tiltScene?.querySelector(".hero-image-frame");
const canHover = window.matchMedia("(hover: hover) and (pointer: fine)");

if (tiltScene && tiltCard && canHover.matches) {
  tiltScene.addEventListener("pointermove", (event) => {
    const bounds = tiltScene.getBoundingClientRect();
    const x = (event.clientX - bounds.left) / bounds.width - 0.5;
    const y = (event.clientY - bounds.top) / bounds.height - 0.5;
    tiltScene.style.setProperty("--tilt-x", `${(-y * 3.5).toFixed(2)}deg`);
    tiltScene.style.setProperty("--tilt-y", `${(x * 4.5).toFixed(2)}deg`);
  });
  tiltScene.addEventListener("pointerleave", () => {
    tiltScene.style.setProperty("--tilt-x", "0deg");
    tiltScene.style.setProperty("--tilt-y", "0deg");
  });
}

const menuButton = document.querySelector(".menu-toggle");
const navigation = document.querySelector(".main-nav");
menuButton?.addEventListener("click", () => {
  const isOpen = menuButton.getAttribute("aria-expanded") === "true";
  menuButton.setAttribute("aria-expanded", String(!isOpen));
  menuButton.setAttribute("aria-label", isOpen ? "Open navigation" : "Close navigation");
  navigation?.classList.toggle("is-open", !isOpen);
});

navigation?.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", () => {
    navigation.classList.remove("is-open");
    menuButton?.setAttribute("aria-expanded", "false");
    menuButton?.setAttribute("aria-label", "Open navigation");
  });
});

if ("IntersectionObserver" in window) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });
  document.querySelectorAll(".craft-image,.craft-copy,.cut-card,.manifesto-image,.manifesto-copy").forEach((element) => {
    element.classList.add("reveal");
    observer.observe(element);
  });
}
