import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
// The kit is the `anatomy` skill's own drawing library (MIT):
//   ~/.agents/skills/anatomy/kit/iso-kit.mjs
//   ~/.agents/skills/anatomy/scripts/inline-kit.mjs
// Build with:  node docs/figures/build-miqat-meter.mjs
import * as k from "../../../.agents/skills/anatomy/kit/iso-kit.mjs";
import { kitScript } from "../../../.agents/skills/anatomy/scripts/inline-kit.mjs";

/**
 * مِقياس الميقات — the app's idea as a bench instrument.
 *
 * A plate carries a vertical ARC (the prayer horizon) on two posts. The arc is
 * ticked with the six prayers. A carriage with a small SUN disc rides the arc
 * at the day's real progress; the part already travelled is gold. A dial gauge
 * in front reads the time left until the next prayer. Everything the readout
 * says comes from the same model that moves the parts.
 */

const HERE = dirname(fileURLToPath(import.meta.url));
const theme = process.argv.includes("--light") ? "light" : "dark";

const WIDTH = 620;
const HEIGHT = 400;

const PLATE = { x: -140, y: -70, w: 290, d: 140, r: 10 };
const PLATE_H = 12;
const FOOT_H = 6;

// the arc (the prayer horizon) — a wide, shallow half-ellipse standing at the
// back of the plate on two posts
const ARC = [10, -30];
const ARC_RX = 118;
const ARC_RZ = 56;
const ARC_BOTTOM = PLATE_H;
const ARC_TOP = ARC_BOTTOM + ARC_RZ;

// the dial gauge (the countdown)
const DIAL = [-96, 26];
const DIAL_R = 21;
const DIAL_Z = 30;
const DIAL_H = 9;

// the prayer stops, as a fraction of the arc from Fajr to Isha
const PRAYERS = [
  ["الفجر", 0.0],
  ["الشروق", 0.12],
  ["الظهر", 0.42],
  ["العصر", 0.66],
  ["المغرب", 0.82],
  ["العشاء", 1.0],
];

const P = k.fitProjection(
  [
    ...k.boxCorners(PLATE, -PLATE_H - FOOT_H, 0),
    [ARC[0] + ARC_RX + 14, ARC[1], ARC_TOP + 10],
    [ARC[0] - ARC_RX - 14, ARC[1], ARC_TOP + 10],
    [DIAL[0] - DIAL_R - 8, DIAL[1], DIAL_Z + DIAL_H],
  ],
  WIDTH,
  HEIGHT,
  { pad: 34, azimuth: 42 },
);

const at = (point) => k.iso(point, P);
const S = (paths, style) => k.solidSvg(paths, style);
const L = (d, style) => k.lineSvg(d, style);
const D = (points, style) => k.dotsSvg(points, style);
const byDepth = (points) =>
  [...points].sort((a, b) => k.depthOf([a[0], a[1], 0], P) - k.depthOf([b[0], b[1], 0], P));

/** The arc runs from Fajr (left) to Isha (right) over the top. */
function arcPoint(fraction) {
  const angle = Math.PI - fraction * Math.PI;
  return [ARC[0] - Math.cos(angle) * ARC_RX, ARC[1]];
}

function arcZ(fraction) {
  const angle = Math.PI - fraction * Math.PI;
  return ARC_BOTTOM + Math.sin(angle) * ARC_RZ;
}

function plate() {
  const out = [`<path class="iso-halo" d="${k.haloOf(PLATE, -PLATE_H - FOOT_H, PLATE_H, P)}"/>`];
  for (const [x, y] of byDepth(k.corners(PLATE, 22))) {
    out.push(S(k.cylinder(x, y, 7, -PLATE_H - FOOT_H, 1.8, P, 28), { tone: "lo" }));
    out.push(S(k.cylinder(x, y, 5.4, -PLATE_H - FOOT_H + 1.8, FOOT_H - 1.8, P, 28), { tone: "mid" }));
  }
  out.push(S(k.slabOf(PLATE, -PLATE_H, PLATE_H, P, 8, 1.6), { tone: "mid" }));
  out.push(L(k.planOutline(k.insetPlan(PLATE, 5), 0, P), { tone: "lo" }));
  const screws = k.corners(PLATE, 10);
  out.push(L(screws.map(([x, y]) => k.ring(x, y, 2.2, 0, P, 16)).join(""), { tone: "lo" }));
  out.push(D(screws.map(([x, y]) => at([x, y, 0])), { size: 0.5 }));
  // a nameplate carrying the Hijri date
  const plate2 = { x: -132, y: PLATE.y + PLATE.d - 18, w: 96, d: 9, r: 1.4 };
  out.push(S(k.slabOf(plate2, 0, 1.4, P, 3, 0.4), { tone: "hi" }));
  out.push(L(k.planOutline(k.insetPlan(plate2, 2), 1.4, P, 3), { tone: "faint" }));
  return out.join("");
}

function posts() {
  const out = [];
  for (const fraction of [0, 1]) {
    const [x, y] = arcPoint(fraction);
    const z = arcZ(fraction);
    out.push(S(k.cylinder(x, y, 5, 0, z, P, 24), { tone: "mid" }));
    out.push(S(k.cylinder(x, y, 6.6, z - 1, 4, P, 24), { tone: "mid" }));
    out.push(L(k.ring(x, y, 3, z + 3, P, 16), { tone: "lo" }));
    out.push(S(k.slabOf({ x: x - 8, y: y - 7, w: 16, d: 14, r: 2 }, 0, 3, P, 4, 0.5), { tone: "mid" }));
    out.push(D([[x - 5, y - 4, 3], [x + 5, y + 4, 3]], { size: 0.45 }));
  }
  const [x0, y0] = arcPoint(0);
  const [x1, y1] = arcPoint(1);
  out.push(S(k.slabOf({ x: x0 + 3, y: y0 - 3.5, w: x1 - x0 - 6, d: 7, r: 1.4 }, 5, 3.4, P, 3, 0.4), { tone: "mid" }));
  out.push(D([[x0 + 8, y0, 8.4], [x1 - 8, y1, 8.4]], { size: 0.45 }));
  return out.join("");
}

function arc() {
  const out = [];
  // the untravelled part of the horizon: a thin steel band
  const band = [];
  const steps = 48;
  for (let i = 0; i <= steps; i++) {
    const fraction = i / steps;
    const [x, y] = arcPoint(fraction);
    band.push(at([x, y, arcZ(fraction)]));
  }
  out.push(`<path class="iso-line" data-tone="lo" d="M${band.map((p) => `${p[0]},${p[1]}`).join(" L")}" style="stroke-width:2.4"/>`);

  // the prayer ticks on the horizon
  PRAYERS.forEach(([label, fraction]) => {
    const [x, y] = arcPoint(fraction);
    const z = arcZ(fraction);
    const angle = Math.PI - fraction * Math.PI;
    const ox = x - Math.cos(angle) * 7;
    const oz = z + Math.sin(angle) * 7;
    out.push(L(k.lineOnTop([x, y], [ox, y], oz, P), { tone: "mid" }));
    out.push(D([at([x, y, z])], { size: 0.6, tone: "hi" }));
    out.push(
      k.faceTextSvg(
        k.topMatrix([ox + (fraction < 0.5 ? -8 : 8), y], oz, P, "x"),
        label.slice(0, 3),
        { size: 5.2, tone: "lo", anchor: "middle" },
      ),
    );
  });
  return out.join("");
}

/** The gold part of the horizon the day has already travelled. */
function travelled(fraction) {
  if (fraction <= 0.001) return "";
  const band = [];
  const steps = Math.max(2, Math.round(48 * fraction));
  for (let i = 0; i <= steps; i++) {
    const f = (i / steps) * fraction;
    const [x, y] = arcPoint(f);
    band.push(at([x, y, arcZ(f)]));
  }
  return `<path class="iso-line" data-tone="lit" data-free="start" d="M${band
    .map((p) => `${p[0]},${p[1]}`)
    .join(" L")}" style="stroke-width:4.6;stroke-linecap:round"/>`;
}

/** The carriage with the sun disc, riding the arc. */
function carriage() {
  const out = [];
  const [x, y] = arcPoint(0);
  const z = arcZ(0);
  out.push(S(k.slabOf({ x: x - 7, y: y - 2.2, w: 14, d: 4.4, r: 1.2 }, z - 7, 8, P, 3, 0.4), { tone: "mid" }));
  out.push(S(k.cylinder(x, y, 1.5, z + 1, 6, P, 16), { tone: "mid", crease: "none" }));
  out.push(S(k.cylinder(x, y, 5.4, z + 7, 2.6, P, 28), { tone: "hi", lit: true }));
  out.push(L(k.ring(x, y, 4, z + 9.6, P, 20), { tone: "lo" }));
  return out.join("");
}

function dial() {
  const out = [];
  const [x, y] = DIAL;
  out.push(S(k.cylinder(x, y, 3.2, 0, DIAL_Z, P, 20), { tone: "mid" }));
  out.push(S(k.cylinder(x, y, DIAL_R, DIAL_Z, DIAL_H, P, 64, 0.8), { tone: "hi", crease: "lo" }));
  out.push(L(k.knurl(x, y, DIAL_R, DIAL_Z + DIAL_H - 3.4, DIAL_Z + DIAL_H, 84, P, 0.3), { tone: "lo" }));
  out.push(L(k.sideArc(x, y, DIAL_R, DIAL_Z + DIAL_H - 3.4, P, 40), { tone: "faint" }));
  out.push(L(k.ring(x, y, DIAL_R - 2.2, DIAL_Z + DIAL_H, P, 64), { tone: "lo" }));
  const ticks = k.radialTicks(x, y, DIAL_R - 3, 12, 3, DIAL_Z + DIAL_H, [1.3, 2.8], P);
  out.push(L(ticks.minor, { tone: "lo" }), L(ticks.major, { tone: "mid" }));
  out.push(L(k.ring(x, y, 1.6, DIAL_Z + DIAL_H, P, 16), { tone: "mid" }));
  out.push(
    k.faceTextSvg(k.topMatrix([x, y + 9], DIAL_Z + DIAL_H, P, "x"), "0", {
      size: 2.4,
      tone: "lo",
      anchor: "middle",
    }),
  );
  return out.join("");
}

const needleAt = (turn) => {
  const angle = turn * Math.PI * 2;
  const [x, y] = DIAL;
  return k.lineOnTop(
    [x - Math.sin(angle) * 3.4, y + Math.cos(angle) * 3.4],
    [x + Math.sin(angle) * (DIAL_R - 4), y - Math.cos(angle) * (DIAL_R - 4)],
    DIAL_Z + DIAL_H + 0.2,
    P,
  );
};

const svg = k.figureSvg({
  width: WIDTH,
  height: HEIGHT,
  label:
    "An instrument for reading a miqat. A plate carries a vertical arc — the horizon from Fajr to Isha — ticked with the six prayers. A carriage holding a small sun disc rides the arc at the day's real progress, and the part of the horizon already travelled is gold. A dial gauge in front reads the time left until the next prayer.",
  body: [
    plate(),
    posts(),
    arc(),
    `<g data-part="carriage">${carriage()}</g>`,
    `<path data-part="travelled" class="iso-line" data-tone="lit" data-free="start" d="${travelled(0.3)}" style="stroke-width:4.6;stroke-linecap:round"/>`,
    dial(),
    `<path data-part="needle" class="iso-line" data-tone="lit" data-free="start" d="${needleAt(0.3)}" style="stroke-width:1.1"/>`,
  ],
});

const live = `
const P = ${JSON.stringify(P)};
const ARC = ${JSON.stringify(ARC)};
const DIAL = ${JSON.stringify(DIAL)};
const DIAL_R = ${DIAL_R}, DIAL_TOP = ${DIAL_Z + DIAL_H};
const PRAYERS = ${JSON.stringify(PRAYERS)};
const STOPS = PRAYERS.map(([, f]) => f);
const arcPoint = (fraction) => {
  const angle = Math.PI - fraction * Math.PI;
  return [ARC[0] - Math.cos(angle) * ${ARC_RX}, ARC[1]];
};
const arcZ = (fraction) => {
  const angle = Math.PI - fraction * Math.PI;
  return ${ARC_BOTTOM} + Math.sin(angle) * ${ARC_RZ};
};
const stage = document.querySelector(".iso-stage");
const svg = stage.querySelector("svg");
const carriage = svg.querySelector('[data-part="carriage"]');
const needle = svg.querySelector('[data-part="needle"]');
const travelled = svg.querySelector('[data-part="travelled"]');
const readout = document.querySelector("[data-readout]");
const still = matchMedia("(prefers-reduced-motion: reduce)");
const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
let fraction = 0.3, target = 0.3, frame = 0, last = 0, clock = 0, touring = true, visible = false, idle = 0;
const tourAt = (t) => STOPS[Math.floor(t / 2.4) % STOPS.length];
const travelledPath = (f) => {
  if (f <= 0.001) return "";
  const steps = Math.max(2, Math.round(48 * f));
  let d = "";
  for (let i = 0; i <= steps; i++) {
    const ff = (i / steps) * f;
    const [x, y] = arcPoint(ff);
    const p = iso([x, y, arcZ(ff)], P);
    d += (i === 0 ? "M" : " L") + p[0] + "," + p[1];
  }
  return d;
};
function draw() {
  const [x, y] = arcPoint(fraction);
  const z = arcZ(fraction);
  const p = iso([x, y, z], P);
  const home = iso([arcPoint(0)[0], arcPoint(0)[1], arcZ(0)], P);
  carriage.setAttribute("transform", "translate(" + (p[0] - home[0]) + "," + (p[1] - home[1]) + ")");
  travelled.setAttribute("d", travelledPath(fraction));
  const angle = fraction * Math.PI * 2;
  const n0 = iso([DIAL[0] - Math.sin(angle) * 3.4, DIAL[1] + Math.cos(angle) * 3.4], P);
  const n1 = iso([DIAL[0] + Math.sin(angle) * (DIAL_R - 4), DIAL[1] - Math.cos(angle) * (DIAL_R - 4)], P);
  needle.setAttribute("d", "M" + n0[0] + "," + n0[1] + " L" + n1[0] + "," + n1[1]);
  const hours = Math.round((1 - fraction) * 12);
  const text = "الوقت الباقي ≈ " + hours + " س  " + (fraction * 100).toFixed(0) + "% من النهار";
  if (readout.textContent !== text) readout.textContent = text;
  stage.setAttribute("aria-valuenow", String(Math.round(fraction * 100)));
  stage.setAttribute("aria-valuetext", text);
}
function tick(now) {
  const dt = last ? Math.min((now - last) / 1000, 1 / 30) : 1 / 60;
  last = now;
  const calm = still.matches;
  if (touring) { clock += dt; target = tourAt(clock); }
  fraction = calm ? target : fraction + (target - fraction) * (1 - Math.exp(-dt / 0.35));
  if (Math.abs(target - fraction) < 0.0005) fraction = target;
  draw();
  const settled = !touring && fraction === target;
  frame = visible && !settled ? requestAnimationFrame(tick) : 0;
}
const run = () => { if (!frame && visible) { last = 0; frame = requestAnimationFrame(tick); } };
const steer = (next) => { touring = false; target = clamp(next, 0, 1); clearTimeout(idle); idle = setTimeout(() => { touring = !still.matches; clock = 0; run(); }, 3600); run(); };
new IntersectionObserver((e) => { visible = e[e.length - 1].isIntersecting; if (visible) run(); }, { rootMargin: "120px 0px" }).observe(stage);
if (still.matches) touring = false;
stage.addEventListener("pointermove", (event) => {
  const box = svg.getBoundingClientRect();
  steer(clamp((event.clientX - box.left) / box.width, 0, 1));
});
stage.addEventListener("pointerleave", () => steer(target));
stage.addEventListener("keydown", (event) => {
  if (event.key === "ArrowRight") steer(clamp(fraction + 0.08, 0, 1));
  if (event.key === "ArrowLeft") steer(clamp(fraction - 0.08, 0, 1));
  if (event.key === "Home") steer(0);
  if (event.key === "End") steer(1);
});
draw();
`;

const body = k.plateHtml({
  fig: "Fig 1",
  title: "مِقياس الميقات",
  hint: "حرّك المؤشّر على القوس.",
  readout: "الوقت الباقي ≈ 8 س",
  keys: [
    { mark: "raised", label: "يتقدّم" },
    { mark: "flat", label: "يتراجع" },
  ],
  caption:
    "القوس من الفجر إلى العشاء، والعلامات هي الصلوات، والقرص الذهبي موضع الشمس الآن.",
  body: `<div class="iso-stage" tabindex="0" role="slider" aria-label="مِقياس الميقات: مفاتيح الأسهم تحرّك قرص الشمس على القوس" aria-valuemin="0" aria-valuemax="100" aria-valuenow="30">${svg}</div>`,
});

const outPath = join(HERE, theme === "light" ? "miqat-meter-light.html" : "miqat-meter.html");
writeFileSync(outPath, k.pageHtml({ title: "مِقياس الميقات", theme, body, script: kitScript() + live }));
console.log("wrote", outPath);
