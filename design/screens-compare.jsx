/* eslint-disable */
// Eurostat wireframes — Comparison mode
// A: Stacked lines (overlay) with country chips
// B: Small multiples (one mini chart per country)

function ScreenCompareA({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  const countries = [
    { c: 'DE', n: 'Germany', color: SK.accent, val: '€44.6k' },
    { c: 'FR', n: 'France',  color: SK.warn,   val: '€38.5k' },
    { c: 'IT', n: 'Italy',   color: SK.ink,    val: '€31.2k' },
  ];
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkIcon name="chev-l" size={20} />
        <span style={{ fontFamily: 'var(--marker)', fontSize: 22 }}>compare</span>
        <div style={{ flex: 1 }} />
        <SkIcon name="refresh" size={18} />
        <SkIcon name="dots" size={20} />
      </div>

      {/* country chips */}
      <div style={{ padding: '8px 16px 0', display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
        {countries.map(({ c, n, color }) => (
          <SkBox key={c} tight style={{ padding: '4px 8px', display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 10, height: 10, borderRadius: '50%', background: color, border: '1.2px solid var(--ink)' }} />
            <SkFlag code={c} />
            <span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>{n}</span>
            <SkIcon name="close" size={12} stroke={SK.muted} />
          </SkBox>
        ))}
        <SkBox tight dashed style={{ padding: '4px 8px', display: 'flex', alignItems: 'center', gap: 4 }}>
          <SkIcon name="plus" size={14} /><span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>add</span>
        </SkBox>
      </div>

      {/* indicator switcher */}
      <div style={{ padding: '8px 16px 0' }}>
        <SkBox tight style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>GDP per capita · €  (PPP)</span>
          <SkIcon name="chev-d" size={14} />
        </SkBox>
      </div>

      {/* the chart */}
      <div style={{ padding: '10px 16px 0' }}>
        <SkBox style={{ padding: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <div className="sk-num" style={{ fontSize: 22 }}>3 lines · 2000—2023</div>
            <div style={{ display: 'flex', gap: 4 }}>
              <SkPill variant="accent">€</SkPill>
              <SkPill>% of '00</SkPill>
            </div>
          </div>
          <SkLineChart height={170} lines={3} />
          {/* legend with values */}
          <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 4 }}>
            {countries.map(({ c, n, color, val }) => (
              <div key={c} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 22, height: 0, borderTop: `2.5px solid ${color}`, display: 'inline-block' }} />
                <span style={{ fontFamily: 'var(--hand)', fontSize: 13, flex: 1 }}>{n}</span>
                <span className="sk-num" style={{ fontSize: 13 }}>{val}</span>
                <span className="sk-num" style={{ fontSize: 11, color: SK.muted, width: 36, textAlign: 'right' }}>+1.4%</span>
              </div>
            ))}
          </div>
        </SkBox>
      </div>

      {/* explanation note */}
      <div style={{ padding: '8px 16px 0' }}>
        <div className="sk-note">
          tap a chart point → exact value & source.<br />
          <strong>scales auto-normalize</strong> when units mix.
        </div>
      </div>

      <div style={{ flex: 1 }} />
      <SkTabBar active="Economy" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// Variant B — small multiples (one mini chart per country)
function ScreenCompareB({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  const countries = [
    { c: 'DE', n: 'Germany', val: '€44.6k', delta: '+1.4', shape: 1 },
    { c: 'FR', n: 'France',  val: '€38.5k', delta: '+0.7', shape: 2 },
    { c: 'IT', n: 'Italy',   val: '€31.2k', delta: '+0.2', shape: 3 },
    { c: 'PL', n: 'Poland',  val: '€18.9k', delta: '+3.6', shape: 4 },
  ];
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkIcon name="chev-l" size={20} />
        <span style={{ fontFamily: 'var(--marker)', fontSize: 22 }}>compare</span>
        <div style={{ flex: 1 }} />
        <SkPill>overlay</SkPill>
        <SkPill variant="accent">small mult.</SkPill>
      </div>

      <div style={{ padding: '6px 16px 4px' }}>
        <SkBox tight style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>GDP per capita · €</span>
          <SkIcon name="chev-d" size={14} />
        </SkBox>
      </div>

      <div style={{ padding: '6px 16px 0', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
        {countries.map(({ c, n, val, delta }, i) => (
          <SkBox key={c} style={{ padding: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <SkFlag code={c} />
              <div style={{ flex: 1 }}>
                <div style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>{n}</div>
                <div className="sk-num" style={{ fontSize: 14 }}>{val}</div>
              </div>
            </div>
            <div style={{ marginTop: 4 }}>
              <SkLineChart height={50} hideAxis lines={1} accent={i === 0 ? SK.accent : SK.ink} />
            </div>
            <div className="sk-num" style={{ fontSize: 10, color: SK.muted, marginTop: 2 }}>YoY {delta}%  ·  '00—'24</div>
          </SkBox>
        ))}
      </div>

      <div style={{ padding: '10px 16px 0' }}>
        <SkBox dashed style={{ padding: 10 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>quick rank</div>
          <SkBars height={90} horizontal label="" />
        </SkBox>
      </div>

      <div style={{ padding: '8px 16px 0' }}>
        <div className="sk-note">
          good for <strong>different scales</strong> — DE vs MT etc.
        </div>
      </div>

      <div style={{ flex: 1 }} />
      <SkTabBar active="Economy" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

Object.assign(window, { ScreenCompareA, ScreenCompareB });
