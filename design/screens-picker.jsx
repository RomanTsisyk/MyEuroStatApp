/* eslint-disable */
// Eurostat wireframes — Country picker
// A: Searchable flag list with recents
// B: Map-based picker

const EU_COUNTRIES = [
  ['AT','Austria'], ['BE','Belgium'], ['BG','Bulgaria'], ['HR','Croatia'],
  ['CY','Cyprus'], ['CZ','Czechia'], ['DK','Denmark'], ['EE','Estonia'],
  ['FI','Finland'], ['FR','France'], ['DE','Germany'], ['GR','Greece'],
  ['HU','Hungary'], ['IE','Ireland'], ['IT','Italy'], ['LV','Latvia'],
  ['LT','Lithuania'], ['LU','Luxembourg'], ['MT','Malta'], ['NL','Netherlands'],
  ['PL','Poland'], ['PT','Portugal'], ['RO','Romania'], ['SK','Slovakia'],
  ['SI','Slovenia'], ['ES','Spain'], ['SE','Sweden'],
];

// ────────────────────────────────────────────────────────────────
// A — Searchable flag list (default) with recents at top
// ────────────────────────────────────────────────────────────────
function ScreenPickerA({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  const recents = [['DE','Germany'], ['FR','France'], ['PL','Poland']];
  const items = EU_COUNTRIES.slice(0, 16);

  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      {/* header row */}
      <div style={{ padding: '6px 16px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--hand)', fontSize: 14 }}>Cancel</span>
        <span style={{ fontFamily: 'var(--marker)', fontSize: 20 }}>pick a country</span>
        <span style={{ fontFamily: 'var(--hand)', fontSize: 14, color: SK.accent }}>Done</span>
      </div>

      {/* search field */}
      <div style={{ padding: '12px 16px 8px' }}>
        <SkBox tight dashed style={{ padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <SkIcon name="search" size={16} stroke={SK.muted} />
          <span style={{ fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted2 }}>search 27 EU + EFTA…</span>
        </SkBox>
        {/* multi-select toggle */}
        <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
          <SkPill variant="accent">single</SkPill>
          <SkPill>compare ⋯</SkPill>
          <SkPill>EFTA</SkPill>
          <SkPill>all 27</SkPill>
        </div>
      </div>

      {/* recents */}
      <div style={{ padding: '4px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted }}>recent</div>
        <div style={{ display: 'flex', gap: 8, marginTop: 6, marginBottom: 8 }}>
          {recents.map(([c, n]) => (
            <SkBox key={c} tight style={{ padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 6 }}>
              <SkFlag code={c} />
              <span style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>{n}</span>
            </SkBox>
          ))}
        </div>
      </div>

      {/* alphabetical list */}
      <div style={{ flex: 1, overflow: 'hidden', padding: '4px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted, marginBottom: 4 }}>all  ·  A — Z</div>
        <div style={{ borderTop: '1px solid var(--ink)' }}>
          {items.slice(0, 9).map(([c, n], i) => (
            <div key={c} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px dashed rgba(31,29,26,0.25)' }}>
              <SkFlag code={c} />
              <span style={{ flex: 1, fontFamily: 'var(--hand)', fontSize: 14 }}>{n}</span>
              <span className="sk-num" style={{ fontSize: 10, color: SK.muted }}>{c}</span>
              <SkIcon name="chev-r" size={14} stroke={SK.muted} />
            </div>
          ))}
        </div>
      </div>

      {/* a→z rail */}
      <div style={{ position: 'absolute', right: 4, top: '40%', display: 'flex', flexDirection: 'column', gap: 2, fontFamily: 'var(--mono)', fontSize: 8, color: SK.muted }}>
        {'ABCDEFGHIJKLMNOPRSTU'.split('').map(l => <span key={l}>{l}</span>)}
      </div>

      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// B — Map picker (tap a country)
// ────────────────────────────────────────────────────────────────
function ScreenPickerB({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '6px 16px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--hand)', fontSize: 14 }}>Cancel</span>
        <span style={{ fontFamily: 'var(--marker)', fontSize: 20 }}>tap a country</span>
        <span style={{ fontFamily: 'var(--hand)', fontSize: 14, color: SK.accent }}>Done</span>
      </div>

      {/* mode toggle */}
      <div style={{ padding: '8px 16px 6px', display: 'flex', gap: 6 }}>
        <SkPill variant="accent">🗺  map</SkPill>
        <SkPill>list</SkPill>
        <SkPill>compare ⋯</SkPill>
      </div>

      {/* the map */}
      <div style={{ padding: '0 16px' }}>
        <SkBox style={{ padding: 4, overflow: 'hidden' }}>
          <SkChoropleth height={240} highlight="DE" />
        </SkBox>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 13, color: SK.muted, marginTop: 4, textAlign: 'center' }}>
          ↑ tap any country  ·  pinch to zoom
        </div>
      </div>

      {/* selected card */}
      <div style={{ padding: '10px 16px 0' }}>
        <SkBox style={{ padding: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <SkFlag code="DE" />
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: 'var(--hand)', fontSize: 14 }}>Germany</div>
              <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>83.2 M people  ·  joined 1958</div>
            </div>
            <SkBtn primary>use this →</SkBtn>
          </div>
        </SkBox>
      </div>

      {/* recents row */}
      <div style={{ padding: '10px 16px 0', display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>recent →</span>
        {['FR','PL','IT','ES'].map(c => (
          <SkBox key={c} tight style={{ padding: '4px 6px', display: 'flex', alignItems: 'center', gap: 4 }}>
            <SkFlag code={c} /><span style={{ fontFamily: 'var(--mono)', fontSize: 11 }}>{c}</span>
          </SkBox>
        ))}
      </div>

      <div style={{ marginTop: 'auto', padding: '10px 16px', fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted2 }}>
        long-press → add to comparison
      </div>

      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

Object.assign(window, { ScreenPickerA, ScreenPickerB, EU_COUNTRIES });
