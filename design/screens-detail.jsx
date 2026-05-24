/* eslint-disable */
// Eurostat wireframes — Population module + generic Module template
// A: Population — pyramid as hero
// B: Generic Module template (reusable structure for all 8 modules)

function ScreenPopulationA({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      {/* header */}
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkIcon name="chev-l" size={20} />
        <div className="sk-pill">
          <SkFlag code="DE" /><span style={{ marginLeft: 4 }}>Germany</span><SkIcon name="chev-d" size={14} />
        </div>
        <div style={{ flex: 1 }} />
        <SkIcon name="plus" size={20} />
      </div>

      <div style={{ padding: '8px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
          <div style={{ fontFamily: 'var(--marker)', fontSize: 28 }}>People</div>
          <span className="sk-pill" style={{ color: SK.muted }}>data 2023</span>
        </div>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>population, demographics, social</div>
      </div>

      {/* hero pyramid */}
      <div style={{ padding: '12px 16px 0' }}>
        <SkBox style={{ padding: 10 }}>
          <SkTag>HERO — demographic pyramid</SkTag>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
            <div>
              <div className="sk-num" style={{ fontSize: 30, lineHeight: 1 }}>83.2<span style={{ fontSize: 18, color: SK.muted, marginLeft: 4 }}>M</span></div>
              <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, marginTop: 2 }}>total population · +0.1% YoY</div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <SkPill>by age</SkPill>
              <SkPill variant="accent">pyramid</SkPill>
            </div>
          </div>
          <SkPyramid height={210} label="age groups, 5-yr cohorts" />
          {/* scrub year */}
          <div style={{ marginTop: 8, padding: '6px 4px 0' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted }}>
              <span>2000</span><span>2010</span><span style={{ color: SK.ink }}>● 2023</span>
            </div>
            <div style={{ height: 22, position: 'relative', marginTop: 4 }}>
              <div style={{ position: 'absolute', left: 0, right: 0, top: 10, height: 2, background: SK.ink }} />
              <div style={{ position: 'absolute', left: '90%', top: 4, width: 14, height: 14, borderRadius: 7, background: SK.accent, border: '1.5px solid var(--ink)' }} />
            </div>
          </div>
        </SkBox>
      </div>

      {/* secondary stats */}
      <div style={{ padding: '10px 16px 0', display: 'flex', gap: 8 }}>
        <SkBox style={{ flex: 1, padding: 10 }}>
          <SkStat label="median age" value="44.6y" delta="▲ +0.2 YoY" />
        </SkBox>
        <SkBox style={{ flex: 1, padding: 10 }}>
          <SkStat label="life expect." value="81.3y" delta="▲ +0.4" />
        </SkBox>
        <SkBox style={{ flex: 1, padding: 10 }}>
          <SkStat label="fertility" value="1.46" delta="▼ −0.03" />
        </SkBox>
      </div>

      {/* secondary visual: total trend */}
      <div style={{ padding: '10px 16px 0' }}>
        <SkBox style={{ padding: 10 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>population over time</div>
          <SkLineChart height={80} />
        </SkBox>
      </div>

      {/* source footer */}
      <div style={{ padding: '8px 16px 8px', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted2 }}>
        eurostat · demo_pjan, demo_mlexpec ·  updated 2026-04
      </div>

      <SkTabBar active="People" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// Generic module detail — reusable template (per brief)
function ScreenTemplate({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkIcon name="chev-l" size={20} />
        <div className="sk-pill"><SkFlag code="DE" /><span style={{ marginLeft: 4 }}>Germany</span><SkIcon name="chev-d" size={14} /></div>
        <div style={{ flex: 1 }} />
        <SkIcon name="filter" size={20} />
      </div>

      <div style={{ padding: '8px 16px 0' }}>
        <div className="sk-wave" style={{ fontFamily: 'var(--marker)', fontSize: 26, display: 'inline-block' }}>[ Module ]</div>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>{'<one-line description from data dictionary>'}</div>
      </div>

      {/* country chips for comparison */}
      <div style={{ padding: '10px 16px 0', display: 'flex', alignItems: 'center', gap: 6 }}>
        <SkBox tight style={{ padding: '4px 8px', display: 'flex', gap: 4, alignItems: 'center' }}>
          <SkFlag code="DE" /><span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>DE</span>
        </SkBox>
        <SkBox tight style={{ padding: '4px 8px', display: 'flex', gap: 4, alignItems: 'center' }}>
          <SkFlag code="FR" /><span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>FR</span>
        </SkBox>
        <SkBox tight dashed style={{ padding: '4px 10px', display: 'flex', gap: 4, alignItems: 'center' }}>
          <SkIcon name="plus" size={14} /><span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>add</span>
        </SkBox>
        <div style={{ flex: 1 }} />
        <span className="sk-pill">log</span>
        <span className="sk-pill">% of EU</span>
      </div>

      {/* primary viz */}
      <div style={{ padding: '10px 16px 0' }}>
        <SkBox style={{ padding: 10 }}>
          <SkTag>PRIMARY · chart slot</SkTag>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
            <div className="sk-num" style={{ fontSize: 26 }}>•••</div>
            <span className="sk-num" style={{ fontSize: 12, color: SK.muted }}>2000 — 2023</span>
          </div>
          <SkLineChart height={110} lines={2} />
        </SkBox>
      </div>

      {/* year scrubber */}
      <div style={{ padding: '10px 16px 0' }}>
        <SkBox dashed style={{ padding: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted }}>
            <span>year range</span><span>2000 — 2023</span>
          </div>
          <div style={{ height: 26, position: 'relative', marginTop: 6 }}>
            <div style={{ position: 'absolute', left: 4, right: 4, top: 12, height: 2, background: SK.muted2 }} />
            <div style={{ position: 'absolute', left: '20%', right: '8%', top: 12, height: 2, background: SK.ink }} />
            <div style={{ position: 'absolute', left: '20%', top: 6, width: 14, height: 14, borderRadius: 7, background: SK.paper, border: '1.5px solid var(--ink)' }} />
            <div style={{ position: 'absolute', right: '8%', top: 6, width: 14, height: 14, borderRadius: 7, background: SK.accent, border: '1.5px solid var(--ink)' }} />
          </div>
        </SkBox>
      </div>

      {/* secondary metrics row */}
      <div style={{ padding: '10px 16px 0', display: 'flex', gap: 8 }}>
        <SkBox style={{ flex: 1, padding: 8 }}><SkStat label="metric A" value="123" delta="▲" /></SkBox>
        <SkBox style={{ flex: 1, padding: 8 }}><SkStat label="metric B" value="4.5%" delta="=" /></SkBox>
        <SkBox style={{ flex: 1, padding: 8 }}><SkStat label="metric C" value="−1.2" delta="▼" /></SkBox>
      </div>

      {/* methodology footer */}
      <div style={{ padding: '8px 16px', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted2 }}>
        source: eurostat · ds-code · last refresh 4h ago
      </div>

      <SkTabBar active="More" />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

Object.assign(window, { ScreenPopulationA, ScreenTemplate });
