const BASE_URL = "http://localhost:8080/api";

export async function fetchRuleSets() {
  const res = await fetch(`${BASE_URL}/rulesets`);
  if (!res.ok) {
    throw new Error("Failed to load rulesets");
  }
  return res.json();
}

export async function scanPath(payload) {
  const res = await fetch(`${BASE_URL}/scan/path`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    throw new Error("Path scan failed");
  }
  return res.json();
}

export async function scanFiles({ files, approvedForAi, ruleset }) {
  const formData = new FormData();
  Array.from(files).forEach((file) => formData.append("files", file));
  formData.append("approvedForAi", String(approvedForAi));
  formData.append("ruleset", ruleset);
  const res = await fetch(`${BASE_URL}/scan/files`, {
    method: "POST",
    body: formData
  });
  if (!res.ok) {
    throw new Error("File scan failed");
  }
  return res.json();
}

export async function scanText({ content, approvedForAi, ruleset }) {
  const res = await fetch(`${BASE_URL}/scan/text`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content, approvedForAi, ruleset })
  });
  if (!res.ok) {
    throw new Error("Text scan failed");
  }
  return res.json();
}

export function reportUrl(scanId) {
  return `${BASE_URL}/report/${scanId}`;
}

export function certificatePdfUrl(scanId) {
  return `${BASE_URL}/certify/${scanId}/pdf`;
}

export function summaryPdfUrl(scanId) {
  return `${BASE_URL}/summary/${scanId}/pdf`;
}
