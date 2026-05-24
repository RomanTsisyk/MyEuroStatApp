/* eslint-disable */
// Eurostat wireframes — Overview dashboard, two variants
// Variant A: Hero stat + module tiles
// Variant B: Compare-first (your country vs EU avg)
// Each is platform-agnostic — wraps inside Android or iOS frame.

// Shared header (sketch-y top bar that sits below the device status bar)
function SkPhoneHeader({ title, sub, trailing }) {
  return (
    <div style={{ padding: '6px 16px 10px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 30, color: SK.ink, lineHeight: 1, letterSpacing: 0.2 }}>{title}</div>
        {sub && <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, marginTop: 4 }}>{sub}</div>}
      </div>
      {trailing}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// Variant A — Hero stat + module tiles
// ────────────────────────────────────────────────────────────────
function ScreenOverviewA({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      {/* country chip row */}
      <div style={{ padding: '6px 16px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="sk-pill">
          <SkFlag code="DE" />
          <span style={{ marginLeft: 4 }}>Germany</span>
          <SkIcon name="chev-d" size={14} />
        </div>
        <SkIcon name="search" size={20} />
      </div>

      <SkPhoneHeader title="27 EU countries" sub="2000 — 2023  ·  Eurostat" />

      {/* hero stat */}
      <div style={{ padding: '0 16px' }}>
        <SkBox style={{ padding: 16 }}>
          <SkTag>HERO — your country at a glance</SkTag>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>GDP · Germany</div>
          <div className="sk-num" style={{ fontSize: 40, lineHeight: 1, marginTop: 4 }}>€4.12<span style={{ fontSize: 22, color: SK.muted, marginLeft: 4 }}>T</span></div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6 }}>
            <span className="sk-num" style={{ color: SK.accent, fontSize: 13 }}>▲ +1.4% YoY</span>
            <span style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>vs EU avg +0.9</span>
          </div>
          <div style={{ marginTop: 10 }}>
            <SkLineChart height={70} hideAxis />
          </div>
        </SkBox>
      </div>

      {/* module tile grid */}
      <div style={{ padding: '14px 16px 0' }}>
        <div className="sk-wave" style={{ fontFamily: 'var(--marker)', fontSize: 18, marginBottom: 8, display: 'inline-block' }}>browse</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          {[
            { name: 'Economy', sub: '€4.1T · +1.4', icon: 'coin' },
            { name: 'People', sub: '83.2 M ·  +0.1', icon: 'people' },
            { name: 'Climate', sub: '6.8 t CO₂ · −3.1', icon: 'leaf' },
            { name: 'Trade', sub: '€1.6T · +0.4', icon: 'globe' },
            { name: 'Transport', sub: '12.4k km · =', icon: 'dots' },
            { name: 'Tourism', sub: '92M nights · +6', icon: 'star' },
          ].map((m, i) => (
            <SkBox key={m.name} style={{ padding: 10 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <SkIcon name={m.icon} size={16} />
                <div style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>{m.name}</div>
              </div>
              <div className="sk-num" style={{ fontSize: 14, marginTop: 4, color: SK.ink }}>{m.sub.split(' · ')[0]}</div>
              <div className="sk-num" style={{ fontSize: 10, color: SK.muted, marginTop: 2 }}>{m.sub.split(' · ')[1]}</div>
            </SkBox>
          ))}
        </div>
      </div>

      {/* footer source */}
      <div style={{ padding: '10px 16px', fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted2, marginTop: 'auto' }}>
        source — eurostat.europa.eu  ·  updated 4h ago
      </div>

      <SkTabBar active="Overview" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// Variant B — Compare-first (your country vs EU avg up top)
// ────────────────────────────────────────────────────────────────
function ScreenOverviewB({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      {/* two-up country comparison header */}
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkBox tight style={{ flex: 1, padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <SkFlag code="DE" />
          <div style={{ minWidth: 0 }}>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>Germany</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 10, color: SK.muted }}>your country</div>
          </div>
        </SkBox>
        <span style={{ fontFamily: 'var(--marker)', fontSize: 22, color: SK.muted }}>vs</span>
        <SkBox tight style={{ flex: 1, padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 26, height: 18, border: '1.2px solid var(--ink)', borderRadius: 2, background: 'var(--accent-soft)', position: 'relative' }}>
            <div style={{ position: 'absolute', inset: 4, border: '1px dashed var(--ink)', borderRadius: 1 }} />
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>EU 27</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 10, color: SK.muted }}>average</div>
          </div>
        </SkBox>
      </div>

      <SkPhoneHeader title="how you compare" sub="6 modules · 12 indicators" />

      {/* comparison rows */}
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 10, flex: 1, overflow: 'hidden' }}>
        {[
          { mod: 'Economy', metric: 'GDP / capita', you: '€44.6k', them: '€37.2k', dir: 'up' },
          { mod: 'People',  metric: 'Life expect.', you: '81.3y',  them: '80.4y', dir: 'up' },
          { mod: 'Climate', metric: 'CO₂ / capita', you: '6.8t',   them: '5.9t',  dir: 'dn' },
          { mod: 'Trade',   metric: 'Exports / GDP',you: '47%',    them: '50%',   dir: 'dn' },
          { mod: 'Social',  metric: 'Unempl.',      you: '3.0%',   them: '6.0%',  dir: 'up' },
        ].map((r, i) => (
          <SkBox key={r.metric} style={{ padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 70 }}>
              <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>{r.mod}</div>
              <div style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>{r.metric}</div>
            </div>
            {/* mini diverging bar */}
            <div style={{ flex: 1, height: 22, position: 'relative' }}>
              <div style={{ position: 'absolute', left: '50%', top: 0, bottom: 0, width: 1, background: SK.muted2 }} />
              {/* you bar */}
              <div style={{ position: 'absolute', top: 3, left: '50%', height: 7, width: `${20 + i * 8}%`,
                background: SK.accent, border: '1px solid var(--ink)' }} />
              {/* them bar */}
              <div style={{ position: 'absolute', bottom: 3, left: '50%', height: 7, width: `${28 - i * 4}%`,
                background: 'transparent', border: '1px dashed var(--ink)' }} />
            </div>
            <div style={{ width: 56, textAlign: 'right' }}>
              <div className="sk-num" style={{ fontSize: 13, color: SK.accent }}>{r.you}</div>
              <div className="sk-num" style={{ fontSize: 10, color: SK.muted }}>vs {r.them}</div>
            </div>
          </SkBox>
        ))}
      </div>

      <div style={{ padding: '8px 16px 10px', fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted2 }}>
        tap a row → full module  ·  swap EU avg for any country
      </div>

      <SkTabBar active="Overview" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

Object.assign(window, { ScreenOverviewA, ScreenOverviewB, SkPhoneHeader });
