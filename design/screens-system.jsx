/* eslint-disable */
// Eurostat wireframes — Search, Settings, Chart library, States grid

// ────────────────────────────────────────────────────────────────
// Search
// ────────────────────────────────────────────────────────────────
function ScreenSearch({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '8px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkBox tight style={{ flex: 1, padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <SkIcon name="search" size={16} stroke={SK.muted} />
          <span style={{ fontFamily: 'var(--hand)', fontSize: 13, flex: 1 }}>youth unemploy<span style={{ animation: 'blink 1s steps(2) infinite' }}>|</span></span>
          <SkIcon name="close" size={14} stroke={SK.muted} />
        </SkBox>
        <span style={{ fontFamily: 'var(--hand)', fontSize: 14, color: SK.accent }}>Cancel</span>
      </div>

      <div style={{ padding: '12px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted }}>indicators</div>
        <SkBox tight style={{ marginTop: 6, padding: 0 }}>
          {[
            ['youth unemployment rate', '15–24, Social', 'une_rt_a'],
            ['unemployment rate, total', 'all ages, Social', 'lfsa_urgan'],
            ['neet rate', 'not in employment, education', 'edat_lfse_20'],
          ].map(([t, s, code], i, arr) => (
            <div key={code} style={{ padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 8, borderBottom: i < arr.length - 1 ? '1px dashed rgba(31,29,26,0.25)' : 'none' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontFamily: 'var(--hand)', fontSize: 14 }}>{t}</div>
                <div style={{ fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted }}>{s} · {code}</div>
              </div>
              <SkIcon name="chev-r" size={14} stroke={SK.muted} />
            </div>
          ))}
        </SkBox>
      </div>

      <div style={{ padding: '12px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted }}>countries matching</div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 6 }}>
          {[['DE','Germany'],['FR','France'],['NL','Netherlands']].map(([c,n]) => (
            <SkBox key={c} tight style={{ padding: '4px 8px', display: 'flex', alignItems: 'center', gap: 4 }}>
              <SkFlag code={c} /><span style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>{n}</span>
            </SkBox>
          ))}
        </div>
      </div>

      <div style={{ padding: '12px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted }}>recent</div>
        <div style={{ marginTop: 6, display: 'flex', flexDirection: 'column', gap: 4 }}>
          {['life expectancy', 'co₂ per capita', 'tourism nights spent'].map(t => (
            <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <SkIcon name="refresh" size={14} stroke={SK.muted} />
              <span style={{ fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted }}>{t}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ flex: 1 }} />
      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// Settings
// ────────────────────────────────────────────────────────────────
function ScreenSettings({ platform }) {
  const topPad = platform === 'ios' ? 56 : 8;
  const row = (label, value, last) => (
    <div style={{ display: 'flex', alignItems: 'center', padding: '12px 12px', borderBottom: last ? 'none' : '1px dashed rgba(31,29,26,0.25)' }}>
      <span style={{ flex: 1, fontFamily: 'var(--hand)', fontSize: 14 }}>{label}</span>
      <span style={{ fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted, marginRight: 6 }}>{value}</span>
      <SkIcon name="chev-r" size={14} stroke={SK.muted} />
    </div>
  );
  const sw = (label, on, last) => (
    <div style={{ display: 'flex', alignItems: 'center', padding: '12px 12px', borderBottom: last ? 'none' : '1px dashed rgba(31,29,26,0.25)' }}>
      <span style={{ flex: 1, fontFamily: 'var(--hand)', fontSize: 14 }}>{label}</span>
      <div style={{ width: 40, height: 22, borderRadius: 11, border: '1.5px solid var(--ink)', background: on ? SK.accent : SK.paper, position: 'relative' }}>
        <div style={{ position: 'absolute', top: 1.5, [on ? 'right' : 'left']: 1.5, width: 16, height: 16, borderRadius: 8, background: SK.paper, border: '1.5px solid var(--ink)' }} />
      </div>
    </div>
  );

  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <div style={{ padding: '6px 16px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <SkIcon name="chev-l" size={20} />
        <span style={{ fontFamily: 'var(--marker)', fontSize: 22 }}>settings</span>
      </div>

      <div style={{ padding: '12px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted, marginBottom: 6 }}>preferences</div>
        <SkBox tight style={{ padding: 0 }}>
          {row('language', 'English')}
          {row('default country', 'Germany 🇩🇪')}
          {row('theme', 'System')}
          {row('units', 'metric · EUR', true)}
        </SkBox>
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted, marginBottom: 6 }}>data</div>
        <SkBox tight style={{ padding: 0 }}>
          {sw('refresh on Wi-Fi only', true)}
          {sw('show stale-data pill', true)}
          {row('cache', '24 MB · clear', true)}
        </SkBox>
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted, marginBottom: 6 }}>about</div>
        <SkBox tight style={{ padding: 0 }}>
          {row('what is Eurostat?', '')}
          {row('how data is collected', '')}
          {row('open source · privacy', '', true)}
        </SkBox>
      </div>

      <div style={{ padding: '14px 16px 8px', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted2, marginTop: 'auto' }}>
        v1.0.0  ·  built with Compose Multiplatform
      </div>

      {platform === 'ios' && <div style={{ height: 34 }} />}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// States — six small phone cards in one wide artboard
// ────────────────────────────────────────────────────────────────
function StateCard({ title, sub, children }) {
  return (
    <div style={{ width: 220, height: 360, position: 'relative' }}>
      <div className="sk sk-paper" style={{
        width: '100%', height: '100%',
        border: '1.5px solid var(--ink)', borderRadius: 22,
        padding: '14px 12px 10px', boxSizing: 'border-box',
        display: 'flex', flexDirection: 'column', gap: 8,
      }}>
        <div style={{ fontFamily: 'var(--marker)', fontSize: 18, lineHeight: 1, color: SK.ink }}>{title}</div>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginTop: -2 }}>{sub}</div>
        <div style={{ flex: 1, marginTop: 4 }}>{children}</div>
      </div>
    </div>
  );
}

function ScreenStates() {
  return (
    <div className="sk sk-paper" style={{ width: '100%', height: '100%', padding: 24, boxSizing: 'border-box', overflow: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 16 }}>
        <div className="sk-wave" style={{ fontFamily: 'var(--marker)', fontSize: 30, display: 'inline-block' }}>States</div>
        <div className="sk-note">six edge & loading states across the app</div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24, justifyItems: 'center' }}>

        {/* Loading shimmer */}
        <StateCard title="loading" sub="skeleton matches final chart shape">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{ height: 16, background: SK.paper2, border: '1px dashed rgba(31,29,26,0.3)', borderRadius: 4, width: '60%' }} />
            <div style={{ height: 28, background: 'repeating-linear-gradient(90deg, var(--paper-2) 0 30px, transparent 30px 50px)', border: '1px dashed rgba(31,29,26,0.3)', borderRadius: 6 }} />
            <div style={{ height: 100, background: 'repeating-linear-gradient(-45deg, rgba(31,29,26,0.06) 0 6px, transparent 6px 12px)', border: '1px dashed rgba(31,29,26,0.3)', borderRadius: 8 }} />
            <div style={{ display: 'flex', gap: 6 }}>
              {[0,1,2].map(i => <div key={i} style={{ flex: 1, height: 44, background: SK.paper2, border: '1px dashed rgba(31,29,26,0.3)', borderRadius: 6 }} />)}
            </div>
          </div>
        </StateCard>

        {/* First launch */}
        <StateCard title="first launch" sub="no country yet — smart default">
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14, marginTop: 30 }}>
            <SkChoropleth height={120} highlight="DE" />
            <div style={{ fontFamily: 'var(--marker)', fontSize: 22, textAlign: 'center' }}>welcome</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 12, textAlign: 'center', color: SK.muted, lineHeight: 1.3 }}>
              we picked <strong style={{ color: SK.ink }}>Germany</strong> based on your region.<br />tap the map to choose.
            </div>
            <SkBtn primary>use Germany →</SkBtn>
          </div>
        </StateCard>

        {/* Offline */}
        <StateCard title="offline" sub="showing cached values">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SkBox tight style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 6, background: 'rgba(176,106,58,0.1)' }}>
              <SkIcon name="wifi-off" size={14} stroke={SK.warn} />
              <span style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.warn }}>offline · cached 14h ago</span>
              <span style={{ flex: 1 }} />
              <span className="sk-num" style={{ fontSize: 10, color: SK.warn }}>retry</span>
            </SkBox>
            <SkBox style={{ padding: 8 }}>
              <div className="sk-num" style={{ fontSize: 18 }}>€4.12T</div>
              <SkLineChart height={70} hideAxis />
              <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted, marginTop: 4 }}>cached · may be stale</div>
            </SkBox>
            <SkBtn full>retry now</SkBtn>
          </div>
        </StateCard>

        {/* No data for combination */}
        <StateCard title="no data" sub="this country-year has no value">
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 14 }}>
            <div style={{ width: 80, height: 60, border: '1.5px dashed var(--ink)', borderRadius: 10, position: 'relative' }}>
              <div style={{ position: 'absolute', inset: 0, background: 'repeating-linear-gradient(-45deg, rgba(31,29,26,0.08) 0 6px, transparent 6px 12px)' }} />
              <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'var(--marker)', fontSize: 30, color: SK.muted2 }}>—</div>
            </div>
            <div style={{ fontFamily: 'var(--marker)', fontSize: 20 }}>no value</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 12, textAlign: 'center', color: SK.muted, lineHeight: 1.3 }}>
              Eurostat doesn't publish <strong style={{ color: SK.ink }}>CY · 2001</strong> for this indicator.
            </div>
            <SkBtn>pick another year</SkBtn>
          </div>
        </StateCard>

        {/* Sparse data — gaps in line */}
        <StateCard title="sparse data" sub="gaps shown explicitly">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SkBox style={{ padding: 8 }}>
              <SkLineChart height={90} gaps />
              <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.warn, marginTop: 2 }}>2009—2010 missing</div>
            </SkBox>
            <SkBox tight style={{ padding: '6px 8px' }}>
              <span style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>methodology change in 2010 — values not directly comparable.</span>
            </SkBox>
            <SkBox tight style={{ padding: '6px 8px', display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ fontFamily: 'var(--hand)', fontSize: 11 }}>interpolate gaps</span>
              <div style={{ width: 30, height: 18, border: '1.5px solid var(--ink)', borderRadius: 9, position: 'relative' }}>
                <div style={{ position: 'absolute', left: 1, top: 1, width: 14, height: 14, borderRadius: 7, background: SK.paper, border: '1px solid var(--ink)' }} />
              </div>
            </SkBox>
          </div>
        </StateCard>

        {/* Stale data pill */}
        <StateCard title="stale" sub="data > 12h old — subtle pill">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SkBox tight style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ width: 8, height: 8, borderRadius: 4, background: SK.warn }} />
              <span style={{ fontFamily: 'var(--hand)', fontSize: 11 }}>data from 13 May · 22h ago</span>
              <span style={{ flex: 1 }} />
              <SkIcon name="refresh" size={12} stroke={SK.muted} />
            </SkBox>
            <SkBox style={{ padding: 8 }}>
              <SkLineChart height={70} hideAxis />
            </SkBox>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, lineHeight: 1.3 }}>
              pull to refresh<br />
              <span style={{ fontFamily: 'var(--mono)', fontSize: 10 }}>↓</span> brings new bytes
            </div>
          </div>
        </StateCard>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// Chart library — wide single artboard
// ────────────────────────────────────────────────────────────────
function ScreenChartLibrary() {
  const Tile = ({ title, sub, children, w = 260, h = 200 }) => (
    <div style={{ width: w }}>
      <div style={{ fontFamily: 'var(--marker)', fontSize: 18, color: SK.ink }}>{title}</div>
      <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginBottom: 6 }}>{sub}</div>
      <SkBox style={{ padding: 8, height: h, boxSizing: 'border-box' }}>{children}</SkBox>
    </div>
  );
  return (
    <div className="sk sk-paper" style={{ width: '100%', height: '100%', padding: 30, boxSizing: 'border-box', overflow: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 16, marginBottom: 6 }}>
        <div className="sk-wave" style={{ fontFamily: 'var(--marker)', fontSize: 32, display: 'inline-block' }}>Chart library</div>
        <div className="sk-note">consistent design language across all 8 modules</div>
      </div>
      <div style={{ fontFamily: 'var(--mono)', fontSize: 11, color: SK.muted, marginBottom: 18 }}>
        every chart: dashed grid · 2px stroke · tabular numerals · neutral by default · color encodes country/category
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 22 }}>
        <Tile title="line" sub="trends over time · Koalaplot">
          <SkLineChart height={150} lines={2} />
        </Tile>
        <Tile title="bar" sub="categorical · top-N · vertical or horizontal">
          <SkBars height={150} horizontal />
        </Tile>
        <Tile title="stacked area" sub="composition over time · emissions / energy">
          <SkArea height={150} />
        </Tile>
        <Tile title="donut" sub="composition snapshot · max 4 slices">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <SkDonut size={130} label="100%" />
          </div>
        </Tile>
        <Tile title="demographic pyramid" sub="custom Canvas · M / F by age" h={250}>
          <SkPyramid height={220} />
        </Tile>
        <Tile title="choropleth map" sub="static SVG · color-shaded countries" h={250}>
          <SkChoropleth height={220} highlight="DE" />
        </Tile>
        <Tile title="sankey" sub="trade flows · partner pairs" h={250}>
          <SkSankey height={220} />
        </Tile>
        <Tile title="heatmap" sub="seasonality · month × year" h={250}>
          <div style={{ paddingTop: 30 }}>
            <SkHeatmap rows={6} cols={12} />
            <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted, marginTop: 6 }}>jan ··············· dec</div>
          </div>
        </Tile>
      </div>

      {/* legend & tooltip conventions */}
      <div style={{ marginTop: 30, display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 22 }}>
        <SkBox style={{ padding: 14 }}>
          <SkTag>legend</SkTag>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted, marginBottom: 8 }}>per-country color, persistent across screens</div>
          {[['DE','#2f4969'],['FR','#b06a3a'],['IT','#1f1d1a']].map(([c, color]) => (
            <div key={c} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
              <span style={{ width: 22, height: 0, borderTop: `3px solid ${color}` }} />
              <span style={{ fontFamily: 'var(--hand)', fontSize: 13 }}>{c}</span>
              <span style={{ flex: 1 }} />
              <span className="sk-num" style={{ fontSize: 12 }}>€44.6k</span>
            </div>
          ))}
        </SkBox>
        <SkBox style={{ padding: 14 }}>
          <SkTag>tap tooltip</SkTag>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted, marginBottom: 8 }}>data-point detail modal</div>
          <SkBox tight dashed style={{ padding: 10, background: SK.paper2 }}>
            <div style={{ fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted }}>2023</div>
            <div className="sk-num" style={{ fontSize: 22 }}>€4 121 B</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>Germany · GDP at current prices</div>
            <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted2, marginTop: 4 }}>eurostat · nama_10_gdp</div>
          </SkBox>
        </SkBox>
        <SkBox style={{ padding: 14 }}>
          <SkTag>number formatting</SkTag>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {[
              ['689 152 000 000 €', '€689B'],
              ['83 200 421 ppl',    '83.2 M'],
              ['0.0146',             '1.46%'],
              ['+0.014',             '▲ +1.4%'],
            ].map(([a, b]) => (
              <div key={a} style={{ display: 'flex', alignItems: 'baseline', gap: 8, borderBottom: '1px dashed rgba(31,29,26,0.2)', padding: '2px 0' }}>
                <span style={{ fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted, flex: 1 }}>{a}</span>
                <span style={{ fontFamily: 'var(--marker)', fontSize: 16, color: SK.muted }}>→</span>
                <span className="sk-num" style={{ fontSize: 14, flex: 1, textAlign: 'right' }}>{b}</span>
              </div>
            ))}
          </div>
        </SkBox>
      </div>
    </div>
  );
}

Object.assign(window, { ScreenSearch, ScreenSettings, ScreenStates, ScreenChartLibrary });
