export const DRAFT_PREFIX = "cookie-oj:draft:v1:";
export const DRAFT_TTL = 30 * 24 * 60 * 60 * 1000;
export const MAX_DRAFT_LENGTH = 65536;
export interface CodeDraft {
  version: 1;
  code: string;
  updatedAt: number;
}

export function draftKey(user: string, question: string, language: string) {
  return (
    DRAFT_PREFIX + [user, question, language].map(encodeURIComponent).join(":")
  );
}

export function readDraft(
  storage: Storage,
  key: string,
  now = Date.now()
): CodeDraft | null {
  const raw = storage.getItem(key);
  if (!raw) return null;
  try {
    const draft = JSON.parse(raw);
    if (
      draft.version === 1 &&
      typeof draft.code === "string" &&
      draft.code.length <= MAX_DRAFT_LENGTH &&
      Number.isFinite(draft.updatedAt) &&
      draft.updatedAt <= now &&
      now - draft.updatedAt <= DRAFT_TTL
    )
      return draft;
  } catch {
    // Corrupt local data must never prevent opening a question.
  }
  storage.removeItem(key);
  return null;
}

export function writeDraft(
  storage: Storage,
  key: string,
  code: string,
  now = Date.now()
) {
  if (code.length > MAX_DRAFT_LENGTH) throw new Error("Draft too large");
  // Limit only this application's drafts, never other local storage entries.
  const drafts: { key: string; updatedAt: number }[] = [];
  for (let i = 0; i < storage.length; i++) {
    const candidate = storage.key(i);
    if (candidate?.startsWith(DRAFT_PREFIX) && candidate !== key) {
      drafts.push({ key: candidate, updatedAt: 0 });
    }
  }
  for (const draft of drafts) {
    draft.updatedAt = readDraft(storage, draft.key, now)?.updatedAt ?? 0;
  }
  drafts.sort((a, b) => b.updatedAt - a.updatedAt);
  drafts.slice(49).forEach((draft) => storage.removeItem(draft.key));
  storage.setItem(key, JSON.stringify({ version: 1, code, updatedAt: now }));
}
