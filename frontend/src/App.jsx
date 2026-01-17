import { useEffect, useMemo, useRef, useState } from "react";
import { fetchRuleSets, scanFiles, scanPath, scanText, reportUrl, certificatePdfUrl, summaryPdfUrl } from "./api.js";

const defaultConfig = {
  path: "",
  approvedForAi: false,
  ruleset: "combined_baseline.yaml",
  scanMode: "upload"
};

export default function App() {
  const [config, setConfig] = useState(defaultConfig);
  const [rulesets, setRulesets] = useState([]);
  const [files, setFiles] = useState([]);
  const [pasteContent, setPasteContent] = useState("");
  const [result, setResult] = useState(null);
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState("");
  const [expandedStep, setExpandedStep] = useState("section-intake-config");
  const [theme, setTheme] = useState("light");
  const [eligibilityChecked, setEligibilityChecked] = useState(false);
  const [remediationViewed, setRemediationViewed] = useState(false);
  const [reportsUnlocked, setReportsUnlocked] = useState(false);
  const profileFileInputRef = useRef(null);

  useEffect(() => {
    fetchRuleSets()
      .then(setRulesets)
      .catch(() => setRulesets([]));
  }, []);

  useEffect(() => {
    const stored = localStorage.getItem("darbiter-theme");
    if (stored === "light" || stored === "dark") {
      setTheme(stored);
      return;
    }
    const prefersDark = window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches;
    setTheme(prefersDark ? "dark" : "light");
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("darbiter-theme", theme);
  }, [theme]);


  const rulesetOptions = useMemo(() => {
    if (rulesets.length === 0) {
      return [{ file: "combined_baseline.yaml", name: "Combined Baseline", version: "1.0" }];
    }
    return rulesets;
  }, [rulesets]);

  const selectedRuleset = useMemo(() => {
    return rulesetOptions.find((item) => item.file === config.ruleset) || rulesetOptions[0];
  }, [rulesetOptions, config.ruleset]);

  const onScan = async () => {
    setStatus("loading");
    setError("");
    setResult(null);
    try {
      const payload = {
        approvedForAi: config.approvedForAi,
        ruleset: config.ruleset
      };
      const data =
        config.scanMode === "path"
          ? await scanPath({ ...payload, path: config.path, recursive: true })
          : config.scanMode === "paste"
          ? await scanText({ ...payload, content: pasteContent })
          : await scanFiles({ ...payload, files });
      setResult(data);
      setExpandedStep("section-findings");
      setStatus("done");
    } catch (err) {
      setError(err.message || "Scan failed");
      setStatus("error");
    }
  };

  const downloadJson = (filename, data) => {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const profileExample = {
    version: 1,
    name: "Example - Combined Baseline",
    config: {
      scanMode: "upload",
      path: "",
      ruleset: "combined_baseline.yaml",
      approvedForAi: false
    }
  };

  const exportProfile = () => {
    const profile = {
      version: 1,
      name: "DArbiter Profile",
      exportedAt: new Date().toISOString(),
      config: {
        scanMode: config.scanMode,
        path: config.path,
        ruleset: config.ruleset,
        approvedForAi: config.approvedForAi
      }
    };
    downloadJson("darbiter-profile.json", profile);
  };

  const downloadExampleProfile = () => {
    downloadJson("darbiter-profile.example.json", profileExample);
  };

  const importProfile = () => {
    profileFileInputRef.current?.click();
  };

  const onProfileSelected = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    try {
      const text = await file.text();
      const parsed = JSON.parse(text);
      const next = parsed?.config;
      if (!next || typeof next !== "object") {
        throw new Error("Invalid profile format: missing config.");
      }
      const nextScanMode = ["upload", "path", "paste"].includes(next.scanMode) ? next.scanMode : "upload";
      setConfig((prev) => ({
        ...prev,
        scanMode: nextScanMode,
        path: typeof next.path === "string" ? next.path : "",
        ruleset: typeof next.ruleset === "string" ? next.ruleset : prev.ruleset,
        approvedForAi: Boolean(next.approvedForAi)
      }));
      setFiles([]);
      setPasteContent("");
    } catch (e) {
      setError(e.message || "Failed to import profile");
      setStatus("error");
    }
  };

  const exportSummary = () => {
    if (!result) return;
    const summary = {
      scanId: result.scanId,
      ruleset: result.ruleset,
      eligibility: result.eligibility,
      usage: result.usage,
      startedAt: result.startedAt,
      finishedAt: result.finishedAt,
      riskSummary: result.riskSummary,
      decision: result.decision,
      findings: result.findings
    };
    downloadJson(`darbiter-summary-${result.scanId}.json`, summary);
  };

  const exportSummaryPdf = () => {
    if (!result) return;
    window.open(summaryPdfUrl(result.scanId), "_blank", "noopener,noreferrer");
  };

  const resetTool = () => {
    setConfig(defaultConfig);
    setFiles([]);
    setPasteContent("");
    setResult(null);
    setStatus("idle");
    setError("");
    setEligibilityChecked(false);
    setRemediationViewed(false);
    setReportsUnlocked(false);
    setExpandedStep("section-intake-config");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const severityBadge = (severity) => {
    if (severity === "CRITICAL") return "danger";
    if (severity === "HIGH") return "warning";
    if (severity === "MEDIUM") return "warning";
    return "success";
  };

  const eligibilityBadge = (eligibility) => {
    if (eligibility === "NOT_AI_SAFE") return "danger";
    if (eligibility === "CONDITIONAL") return "warning";
    if (eligibility === "RESTRICTED") return "warning";
    return "success";
  };

  const hasResult = Boolean(result);

  const steps = [
    { id: "section-intake-config", title: "Intake & Configuration", requiresResult: false },
    { id: "section-findings", title: "Findings Summary", requiresResult: true },
    { id: "section-eligibility", title: "Eligibility Decision", requiresResult: false },
    { id: "section-remediation", title: "Remediation Guidance", requiresResult: false },
    { id: "section-reports", title: "Certification & Reports", requiresResult: false }
  ];

  const canOpenStep = (stepId) => {
    const step = steps.find((item) => item.id === stepId);
    if (!step) return false;
    if (step.id === "section-findings" && !result) return false;
    if (step.id === "section-eligibility" && !eligibilityChecked) return false;
    if (step.id === "section-remediation" && !remediationViewed) return false;
    if (step.id === "section-reports" && !reportsUnlocked) return false;
    if (step.requiresResult && !result) return false;
    return true;
  };

  const goToStep = (stepId) => {
    if (canOpenStep(stepId)) {
      const scrollY = window.scrollY;
      setExpandedStep((prev) => (prev === stepId ? "" : stepId));
      requestAnimationFrame(() => {
        window.scrollTo({ top: scrollY });
      });
    }
  };

  const goToNext = () => {
    const currentIndex = steps.findIndex((step) => step.id === expandedStep);
    for (let i = currentIndex + 1; i < steps.length; i += 1) {
      if (canOpenStep(steps[i].id)) {
        setExpandedStep(steps[i].id);
        return;
      }
    }
  };

  const goToPrev = () => {
    const currentIndex = steps.findIndex((step) => step.id === expandedStep);
    for (let i = currentIndex - 1; i >= 0; i -= 1) {
      if (canOpenStep(steps[i].id)) {
        setExpandedStep(steps[i].id);
        return;
      }
    }
  };

  return (
    <div className="app">
      <div className="topbar">
        <div className="brand">
          <div className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 44 44" role="img" aria-label="DArbiter">
              <defs>
                <linearGradient id="darbiter-gradient" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor="#1d4ed8" />
                  <stop offset="100%" stopColor="#60a5fa" />
                </linearGradient>
              </defs>
              <circle cx="22" cy="22" r="20" fill="url(#darbiter-gradient)" />
              <path
                d="M14 26.5V17.5C14 15.6 15.6 14 17.5 14H23.8C28.3 14 31 16.7 31 22C31 27.3 28.3 30 23.8 30H17.5C15.6 30 14 28.4 14 26.5Z"
                fill="#ffffff"
                fillOpacity="0.95"
              />
              <path
                d="M26.2 18.4C24.4 18 22.4 17.9 20.4 18.2C19.6 18.3 19 19 19 19.8V24.2C19 25 19.6 25.7 20.4 25.8C22.4 26.1 24.4 26 26.2 25.6C27.1 25.4 27.7 24.6 27.7 23.7V20.3C27.7 19.4 27.1 18.6 26.2 18.4Z"
                fill="#0f172a"
                fillOpacity="0.2"
              />
            </svg>
          </div>
          <div className="brand-text">
            <span className="brand-title">DArbiter</span>
            <span className="brand-subtitle">Desktop AI Readiness Arbiter</span>
          </div>
        </div>
        <div className="top-actions">
          <button
            className="ghost theme-toggle"
            type="button"
            aria-label="Toggle dark mode"
            onClick={() => setTheme((prev) => (prev === "light" ? "dark" : "light"))}
          >
            {theme === "light" ? (
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="12" r="4" fill="currentColor" />
                <path
                  d="M12 2.5V5M12 19v2.5M2.5 12H5M19 12h2.5M4.6 4.6l1.8 1.8M17.6 17.6l1.8 1.8M4.6 19.4l1.8-1.8M17.6 6.4l1.8-1.8"
                  stroke="currentColor"
                  strokeWidth="1.6"
                  strokeLinecap="round"
                />
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M20.7 15.1a8.1 8.1 0 0 1-10.8-10.8 1 1 0 0 0-1.3-1.3 10 10 0 1 0 13.4 13.4 1 1 0 0 0-1.3-1.3Z"
                  fill="currentColor"
                />
              </svg>
            )}
          </button>
          <button className="ghost" type="button" onClick={resetTool}>
            Start Scan
          </button>
        </div>
      </div>

      <main className="content">

      <div className="stepper">
        {steps.filter((step) => canOpenStep(step.id)).map((step, index) => {
          const isExpanded = expandedStep === step.id;
          const isComplete =
            (step.id === "section-intake-config" && result) ||
            (step.id === "section-findings" && eligibilityChecked) ||
            (step.id === "section-eligibility" && remediationViewed) ||
            (step.id === "section-remediation" && reportsUnlocked);
          return (
            <section
              key={step.id}
              className={`panel accordion ${isExpanded ? "expanded" : ""}`}
              id={step.id}
            >
              <button
                className="accordion-header"
                type="button"
                onClick={() => goToStep(step.id)}
              >
                <div className="accordion-step">
                  <span className={`step-index ${isComplete ? "done" : ""}`}>
                    {isComplete ? "✓" : index + 1}
                  </span>
                  <div>
                    <p className="section-eyebrow">Step {index + 1}</p>
                    <h2>{step.title}</h2>
                  </div>
                </div>
              </button>
              <div className="accordion-body">
                {step.id === "section-intake-config" && (
        <section className="panel-inner">
          <div className="section-header">
            <div>
              <p className="section-subtitle">
                Select data and configure scan strictness in one step.
              </p>
            </div>
            <div className="section-actions">
              <input
                ref={profileFileInputRef}
                type="file"
                accept="application/json,.json"
                style={{ display: "none" }}
                onChange={onProfileSelected}
              />
              <button className="ghost" type="button" onClick={downloadExampleProfile}>
                Example Profile
              </button>
              <button className="ghost" type="button" onClick={exportProfile}>
                Export Profile
              </button>
              <button className="ghost" type="button" onClick={importProfile}>
                Import Profile
              </button>
            </div>
          </div>
        <div className="intake-options">
          <button
            type="button"
            className={`option-card ${config.scanMode === "upload" ? "active" : ""}`}
            onClick={() => setConfig((prev) => ({ ...prev, scanMode: "upload" }))}
          >
            <span className="option-icon">⬆</span>
            <div>
              <strong>Upload Files</strong>
              <p>Select one or more files for scanning.</p>
            </div>
          </button>
          <button
            type="button"
            className={`option-card ${config.scanMode === "path" ? "active" : ""}`}
            onClick={() => setConfig((prev) => ({ ...prev, scanMode: "path" }))}
          >
            <span className="option-icon">📁</span>
            <div>
              <strong>Scan Folder / File Path</strong>
              <p>Scan a local folder or file path.</p>
            </div>
          </button>
          <button
            type="button"
            className={`option-card ${config.scanMode === "paste" ? "active" : ""}`}
            onClick={() => setConfig((prev) => ({ ...prev, scanMode: "paste" }))}
          >
            <span className="option-icon">📋</span>
            <div>
              <strong>Paste Content</strong>
              <p>Paste JSON, CSV, XML, or text.</p>
            </div>
          </button>
        </div>

        {config.scanMode === "upload" && (
          <div className="card compact">
            <strong>Upload Files</strong>
            <label className="dropzone">
              <input
                type="file"
                multiple
                onChange={(event) => setFiles(event.target.files)}
              />
              <div>
                <p className="dropzone-title">Drag & drop files here</p>
                <p className="dropzone-subtitle">or click to browse</p>
              </div>
            </label>
            {files.length > 0 && (
              <div className="file-list">
                <p className="file-count">
                  {files.length} file(s) selected ·{" "}
                  {(
                    Array.from(files).reduce((total, file) => total + file.size, 0) /
                    (1024 * 1024)
                  ).toFixed(2)}{" "}
                  MB
                </p>
                <ul>
                  {Array.from(files).map((file) => (
                    <li key={`${file.name}-${file.size}`}>
                      {file.name} · {(file.size / 1024).toFixed(1)} KB
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <span className="badge success">
              Approved types: PDF, DOCX, XLSX, JSON, CSV, XML, YAML, TOML, TXT, LOG, ENV, PROPERTIES, CONF
            </span>
          </div>
        )}

        {config.scanMode === "path" && (
          <div className="card compact">
            <strong>Scan Folder / File Path</strong>
            <input
              className="input"
              type="text"
              placeholder="C:\\data\\documents"
              value={config.path}
              onChange={(event) => setConfig((prev) => ({ ...prev, path: event.target.value }))}
            />
            {config.path.trim() && (
              <div className="file-list">
                <p className="file-count">Path selected</p>
                <ul>
                  <li>{config.path}</li>
                </ul>
              </div>
            )}
            <span className="badge warning">Recursive scan enabled</span>
          </div>
        )}

        {config.scanMode === "paste" && (
          <div className="card compact">
            <strong>Paste Content</strong>
            <textarea
              className="input textarea"
              placeholder="Paste JSON / CSV / XML / text here"
              value={pasteContent}
              onChange={(event) => setPasteContent(event.target.value)}
              rows={6}
            />
            {pasteContent.trim() && (
              <div className="file-list">
                <p className="file-count">
                  {pasteContent.trim().split(/\s+/).length} words · {pasteContent.length} characters
                </p>
                <div className="preview-box">
                  {pasteContent.slice(0, 320)}
                  {pasteContent.length > 320 ? "…" : ""}
                </div>
              </div>
            )}
          </div>
        )}

        <div className="divider" />

        <div className="grid two">
            <div className="card">
              <div className="card-title-row">
                <strong>Scan Strictness</strong>
                <span
                  className="info-dot"
                  role="img"
                  aria-label="Info"
                  title="Controls which ruleset/policy pack is applied. Stricter rulesets detect more issues and may produce more findings."
                >
                  i
                </span>
              </div>
              <p className="helper-text">
                Select the policy pack to apply. Use “Combined Baseline” unless you have a custom rule set.
              </p>
              <select
                className="input"
                value={config.ruleset}
                onChange={(event) => setConfig((prev) => ({ ...prev, ruleset: event.target.value }))}
              >
                {rulesetOptions.map((ruleset) => (
                  <option key={ruleset.file} value={ruleset.file}>
                    {ruleset.name} (v{ruleset.version})
                  </option>
                ))}
              </select>
              <div className="row">
                <span className="badge success">PII</span>
                <span className="badge success">Secrets</span>
                <span className="badge warning">Config Risks</span>
              </div>
            </div>
            <div className="card">
              <div className="card-title-row">
                <strong>AI Usage</strong>
                <span
                  className="info-dot"
                  role="img"
                  aria-label="Info"
                  title="Indicates whether this dataset is approved for AI usage. If unchecked and PII is found, eligibility becomes CONDITIONAL."
                >
                  i
                </span>
              </div>
              <p className="helper-text">
                Mark if this data is already approved for AI use. Leave unchecked if unsure.
              </p>
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={config.approvedForAi}
                  onChange={(event) =>
                    setConfig((prev) => ({ ...prev, approvedForAi: event.target.checked }))
                  }
                />
                Approved for AI usage
              </label>
              <div className="row">
                <span className="badge success">Inference</span>
                <span className="badge warning">Training</span>
              </div>
            </div>
          </div>
        <div className="footer-actions">
          <button className="primary" onClick={onScan} disabled={status === "loading"}>
            ▶ Run AI Readiness Scan
          </button>
        </div>
        </section>
                )}

                {step.id === "section-findings" && (
        <section className="panel-inner">
          <div className="section-header">
            <div>
              <p className="section-subtitle">
                Review sensitive findings, severity distribution, and AI risk context.
              </p>
            </div>
            <div className="section-actions">
              <button className="ghost" type="button" onClick={exportSummaryPdf} disabled={!result}>
                Export PDF
              </button>
              <button className="ghost" type="button" onClick={exportSummary} disabled={!result}>
                Export JSON
              </button>
            </div>
          </div>
        {status === "loading" && <p>Scanning...</p>}
        {status === "error" && <p className="error">{error}</p>}
        {result && (
          <div className="results">
            <div className="row">
              <span className={`badge ${eligibilityBadge(result.eligibility)}`}>
                {result.eligibility}
              </span>
              <span className="badge warning">Overall Risk: {result.riskSummary.overall}</span>
              <span className="badge success">Ruleset: {result.ruleset}</span>
            </div>
            <div className="grid four">
              <div className="kpi">
                <span>Total Findings</span>
                <strong>{result.riskSummary.totalFindings}</strong>
              </div>
              <div className="kpi">
                <span>Critical</span>
                <strong>{result.riskSummary.critical}</strong>
              </div>
              <div className="kpi">
                <span>High</span>
                <strong>{result.riskSummary.high}</strong>
              </div>
              <div className="kpi">
                <span>Medium</span>
                <strong>{result.riskSummary.medium}</strong>
              </div>
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th>Location</th>
                  <th>Category</th>
                  <th>Severity</th>
                  <th>Finding</th>
                </tr>
              </thead>
              <tbody>
                {result.findings.map((finding, index) => (
                  <tr key={`${finding.id}-${index}`}>
                    <td>{finding.filePath}</td>
                    <td>{finding.category}</td>
                    <td>
                      <span className={`badge ${severityBadge(finding.severity)}`}>
                        {finding.severity}
                      </span>
                    </td>
                    <td>{finding.label}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="footer-actions">
              <button
                className="primary"
                type="button"
                onClick={() => {
                  setEligibilityChecked(true);
                  setExpandedStep("section-eligibility");
                }}
              >
                ▶ Evaluate AI Eligibility
              </button>
            </div>
          </div>
        )}
        </section>
                )}

                {step.id === "section-eligibility" && result && (
        <section className="panel-inner">
          <div className="section-header">
            <div>
              <p className="section-subtitle">
                Deterministic decision with policy references and rationale.
              </p>
            </div>
          </div>
          <details className="help-details">
            <summary>What does “Policy References” and “Decision” mean?</summary>
            <div className="help-details-body">
              <p>
                <strong>Decision</strong> is the eligibility outcome for AI usage (AI_SAFE, CONDITIONAL,
                NOT_AI_SAFE, RESTRICTED).
              </p>
              <p>
                <strong>Policy References</strong> are the policy rules that justify the decision (for
                audit and compliance). They explain <em>why</em> the data is or isn’t AI‑ready.
              </p>
            </div>
          </details>
        <div className="row">
          <span className={`badge ${eligibilityBadge(result?.eligibility || "NOT_AI_SAFE")}`}>
            {hasResult ? result.eligibility : "NOT_AI_SAFE"}
          </span>
          <span className="badge warning">
            Overall Risk: {hasResult ? result.riskSummary.overall : "HIGH"}
          </span>
        </div>
        <div className="grid two">
          <div className="card">
            <div className="card-title-row">
              <strong>Decision Rationale</strong>
              <span
                className="info-dot"
                role="img"
                aria-label="Info"
                title="Plain-language explanation of why this eligibility status was chosen."
              >
                i
              </span>
            </div>
            <ul>
              {(result?.decision?.reasons?.length
                ? result.decision.reasons
                : [
                    "Critical secrets detected in production configs",
                    "PII present without approval flag",
                    "Config risks could expose data downstream"
                  ]
              ).map((reason, index) => (
                <li key={`reason-${index}`}>{reason}</li>
              ))}
            </ul>
          </div>
          <div className="card">
            <div className="card-title-row">
              <strong>Policy References</strong>
              <span
                className="info-dot"
                role="img"
                aria-label="Info"
                title="These are policy IDs/rules used to support the decision for auditability."
              >
                i
              </span>
            </div>
            <ul>
              {(result?.decision?.policyReferences?.length
                ? result.decision.policyReferences
                : ["POL-SEC-001", "POL-PII-002", "POL-CONFIG-003"]
              ).map((policy, index) => (
                <li key={`policy-${index}`}>{policy}</li>
              ))}
            </ul>
          </div>
        </div>
        <div className="footer-actions">
          <button
            className="primary"
            type="button"
            onClick={() => {
              setRemediationViewed(true);
              setExpandedStep("section-remediation");
            }}
          >
            View Remediation Guidance
          </button>
        </div>
        </section>
                )}

                {step.id === "section-remediation" && result && (
        <section className="panel-inner">
          <div className="section-header">
            <div>
              <p className="section-subtitle">
                Recommended, human‑led remediation steps per finding.
              </p>
            </div>
          </div>
        <div className="grid two">
          {(() => {
            const raw = result?.remediation?.length
              ? result.remediation
              : [
                  {
                    label: "AWS Access Key",
                    actions: ["Mask Value", "Rotate Secret", "Exclude File"]
                  },
                  {
                    label: "Email Address",
                    actions: ["Mask Value", "Replace with Synthetic"]
                  }
                ];

            const merged = new Map();
            for (const item of raw) {
              const key = (item.label || "").trim().toLowerCase();
              const existing = merged.get(key);
              const actions = Array.from(new Set(item.actions || []));
              if (!existing) {
                merged.set(key, { label: item.label, actions });
              } else {
                merged.set(key, {
                  label: existing.label || item.label,
                  actions: Array.from(new Set([...(existing.actions || []), ...actions]))
                });
              }
            }

            return Array.from(merged.values()).map((item, index) => (
              <div className="card" key={`remediation-${index}`}>
                <strong>{item.label}</strong>
                <div className="row">
                  {item.actions.map((action, actionIndex) => (
                    <span className="badge warning" key={`action-${index}-${actionIndex}`}>
                      {action}
                    </span>
                  ))}
                </div>
              </div>
            ));
          })()}
        </div>
        <div className="footer-actions">
          <button
            className="primary"
            type="button"
            onClick={() => {
              setReportsUnlocked(true);
              setExpandedStep("section-reports");
            }}
          >
            Continue to Certification
          </button>
        </div>
        </section>
                )}

                {step.id === "section-reports" && result && (
        <section className="panel-inner">
          <div className="section-header">
            <div>
              <p className="section-subtitle">
                Generate AI readiness certificate and audit artifacts.
              </p>
            </div>
          </div>
        <div className="grid two">
          <div className="card">
            <strong>AI Readiness Certificate</strong>
            <p>Scope: /exports/customer_data</p>
            <p>Status: APPROVED</p>
            <p>Usage: Inference</p>
            <p>Ruleset: Combined Baseline v1.0</p>
            <p>Date: 2026-01-15</p>
          </div>
          <div className="card">
            <strong>Reports</strong>
            <div className="row">
              <span className="badge success">Executive Summary</span>
              <span className="badge success">Detailed Findings</span>
              <span className="badge success">AI Readiness Report</span>
            </div>
          </div>
        </div>
        <div className="footer-actions">
          <a className="primary" href={certificatePdfUrl(result?.scanId || "")} target="_blank" rel="noreferrer">
            Download Certificate (PDF)
          </a>
          <button className="ghost" type="button">
            Generate Audit Report
          </button>
        </div>
        </section>
                )}
              </div>
            </section>
          );
        })}
      </div>
        </main>
    </div>
  );
}
