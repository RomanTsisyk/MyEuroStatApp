/* eslint-disable */
// 8 feature-module phone screens. One section per module from the handoff spec.
// Each module showcases a *different* metric-switcher pattern (the variation axis).

// Shared chrome wrapper so we don't repeat header + tab strip + footer.
function FeatFrame({ platform, name, children, dataset, isStale = false, tab = 'More' }) {
  const topPad = platform === 'ios' ? 56 : 6;
  const accent = MOD_META[name].accent;
  return (
    <div className="sk sk-paper" style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: topPad }}>
      <ModuleAppBar name={name} accent={accent} />
      <ModuleTabStrip active={name} />
      <div style={{ flex: 1, overflow: 'hidden' }}>{children}</div>
      <SourceFooter dataset={dataset} isStale={isStale} />
      <FeatTabBar active={tab} />
      {platform === 'ios' && <div style={{ height: 28 }} />}
    </div>
  );
}

// Section title helper — bigass marker tagline at top.
function ModuleHeadline({ name, value, unit, sub, accent }) {
  return (
    <div style={{ padding: '8px 14px 0', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
      <div>
        <div className="sk-num" style={{ fontSize: 30, lineHeight: 1, color: accent }}>{value}<span style={{ fontSize: 16, color: SK.muted, marginLeft: 4 }}>{unit}</span></div>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, marginTop: 3 }}>{sub}</div>
      </div>
      <div className="sk-pill" style={{ fontSize: 11, padding: '2px 8px' }}>
        <span style={{ width: 6, height: 6, borderRadius: 3, background: SK.muted2 }} />
        {MOD_META[name].year}
      </div>
    </div>
  );
}

/* ─────────────────────────── 1 · POPULATION ────────────────────────── */
function ScreenFeatPopulation({ platform }) {
  const accent = MOD_META.Population.accent;
  return (
    <FeatFrame platform={platform} name="Population" tab="People" dataset="demo_pjan">
      <ModuleHeadline name="Population" value="83.2" unit="M" sub="total · Germany · ▲ +0.1% YoY" accent={accent} />

      {/* Metric switcher — two-state toggle MEN | WOMEN | ALL */}
      <div style={{ padding: '10px 14px 0' }}>
        <SkSegmented items={['Total', 'Men', 'Women']} active="Total" accent={accent} />
      </div>

      {/* Hero pyramid */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>HERO · demographic pyramid</SkTag>
          <SkPyramid height={184} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4, fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>
            <span>5-yr cohorts</span>
            <span><span className="sk-dot" style={{ background: SK.accent, marginRight: 4 }} />men 40.9M</span>
            <span><span className="sk-dot" style={{ background: SK.warn, marginRight: 4 }} />women 42.3M</span>
          </div>
        </SkBox>
      </div>

      {/* Year slider (single-year for pyramid) */}
      <div style={{ padding: '8px 14px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted }}>year</span>
        <div style={{ flex: 1, height: 14, position: 'relative' }}>
          <div style={{ position: 'absolute', left: 0, right: 0, top: 6, height: 2, background: SK.muted2 }} />
          <div style={{ position: 'absolute', left: '95%', top: 0, width: 14, height: 14, borderRadius: 7, background: accent, border: '1.5px solid var(--ink)' }} />
        </div>
        <span className="sk-num" style={{ fontSize: 13 }}>2024</span>
      </div>

      <CountryChips countries={['DE', 'FR', 'PL', 'EU27']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 2 · ECONOMY ────────────────────────────── */
function ScreenFeatEconomy({ platform }) {
  const accent = MOD_META.Economy.accent;
  return (
    <FeatFrame platform={platform} name="Economy" tab="Economy" dataset="nama_10_gdp · +2">
      <ModuleHeadline name="Economy" value="3 451" unit="B €" sub="GDP · current prices · ▲ +6.2%" accent={accent} />

      {/* Metric switcher — segmented control over 3 KPIs (this is the canonical one) */}
      <div style={{ padding: '10px 14px 0' }}>
        <SkSegmented items={['GDP', 'Inflation', 'Deficit']} active="GDP" accent={accent} />
      </div>

      {/* Hero: multi-country line */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>HERO · GDP €M, current prices</SkTag>
          <SkLineChart height={104} lines={3} accent={accent} accent2="#7a5c46" accent3="#5e6b58" />
          <div style={{ display: 'flex', gap: 10, marginTop: 4, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, flexWrap: 'wrap' }}>
            <span><span className="sk-dot" style={{ background: accent, marginRight: 4 }} />DE</span>
            <span><span className="sk-dot" style={{ background: '#7a5c46', marginRight: 4 }} />FR</span>
            <span><span className="sk-dot" style={{ background: '#5e6b58', marginRight: 4 }} />PL</span>
          </div>
        </SkBox>
      </div>

      {/* Secondary KPI row showing the *other* two metrics — informs user they exist */}
      <div style={{ padding: '8px 14px 0', display: 'flex', gap: 6 }}>
        <SkBox tight style={{ flex: 1, padding: 8, opacity: 0.55 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>HICP infl.</div>
          <div className="sk-num" style={{ fontSize: 16 }}>105.8</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>idx · 2015=100</div>
        </SkBox>
        <SkBox tight style={{ flex: 1, padding: 8, opacity: 0.55 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>deficit</div>
          <div className="sk-num" style={{ fontSize: 16, color: SK.warn }}>−2.5%</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>% of GDP</div>
        </SkBox>
      </div>

      <YearScrubber from={2010} to={2024} />
      <CountryChips countries={['DE', 'FR', 'PL', 'EU27']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 3 · ENVIRONMENT ────────────────────────── */
function ScreenFeatEnvironment({ platform }) {
  const accent = MOD_META.Environment.accent;
  return (
    <FeatFrame platform={platform} name="Environment" tab="Climate" dataset="env_air_gge · +2" isStale>
      <StaleBanner />
      <ModuleHeadline name="Environment" value="733" unit="Mt" sub="GHG · CO₂-eq · TOTAL sector" accent={accent} />

      {/* Two switchers: chip-row (sector) + dropdown (metric) — this is the most-controls module */}
      <div style={{ padding: '10px 14px 0', display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
        <SkDropdown label="metric:" value="GHG" />
        <SkChipRow items={['TOTAL', 'TRANSPORT', 'INDUSTRY']} active="TOTAL" accent={accent} />
      </div>

      {/* Hero */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>GHG emissions · Mt CO₂-eq</SkTag>
          <SkLineChart height={100} lines={2} accent={accent} accent2="#b06a3a" />
        </SkBox>
      </div>

      {/* Secondary — the two hidden metrics inline */}
      <div style={{ padding: '8px 14px 0', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6 }}>
        <SkBox tight style={{ padding: 8 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>energy use</div>
          <div className="sk-num" style={{ fontSize: 15 }}>194 248</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>ktoe</div>
        </SkBox>
        <SkBox tight style={{ padding: 8 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>SDG 13 idx</div>
          <div className="sk-num" style={{ fontSize: 15 }}>58.3</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>1990=100</div>
        </SkBox>
      </div>

      <CountryChips countries={['DE', 'FR', 'PL', 'EU27']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 4 · TRADE ──────────────────────────────── */
function ScreenFeatTrade({ platform }) {
  const accent = MOD_META.Trade.accent;
  return (
    <FeatFrame platform={platform} name="Trade" dataset="ext_lt_intratrd">
      <ModuleHeadline name="Trade" value="+89" unit="B €" sub="balance · intra-EU · partner EU27" accent={accent} />

      {/* Switcher: tabs */}
      <div style={{ padding: '10px 14px 0' }}>
        <SkTabs items={['Exports', 'Imports', 'Balance']} active="Balance" accent={accent} />
      </div>

      {/* Hero: diverging bars exports vs imports */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>exports vs imports · €M</SkTag>
          <SkDivergingBars height={140} accent={accent} warn="#b06a3a" />
          <div style={{ display: 'flex', gap: 12, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginTop: 4 }}>
            <span><span className="sk-dot" style={{ background: accent, marginRight: 4 }} />exports</span>
            <span><span className="sk-dot" style={{ background: '#b06a3a', marginRight: 4 }} />imports</span>
          </div>
        </SkBox>
      </div>

      <div style={{ padding: '8px 14px 0', display: 'flex', gap: 6 }}>
        <SkBox tight style={{ flex: 1, padding: 8 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>exports</div>
          <div className="sk-num" style={{ fontSize: 15, color: accent }}>1 612 B €</div>
        </SkBox>
        <SkBox tight style={{ flex: 1, padding: 8 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>imports</div>
          <div className="sk-num" style={{ fontSize: 15, color: '#b06a3a' }}>1 523 B €</div>
        </SkBox>
      </div>

      <CountryChips countries={['DE', 'FR', 'PL']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 5 · TRANSPORT ──────────────────────────── */
function ScreenFeatTransport({ platform }) {
  const accent = MOD_META.Transport.accent;
  return (
    <FeatFrame platform={platform} name="Transport" dataset="road_pa_buscoa · +1">
      <ModuleHeadline name="Transport" value="4.1" unit="bn" sub="road · passengers · Germany" accent={accent} />

      {/* Switcher: pill toggle ROAD | AIR + log-scale toggle (handles diff orders of magnitude) */}
      <div style={{ padding: '10px 14px 0', display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'space-between' }}>
        <SkPillToggle items={['ROAD', 'AIR', 'ALL']} active="ALL" accent={accent} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>
          log <span style={{
            width: 26, height: 14, borderRadius: 7, border: '1.5px solid var(--ink)', background: accent,
            position: 'relative',
          }}>
            <span style={{ position: 'absolute', right: 1, top: 1, width: 10, height: 10, borderRadius: 5, background: SK.paper, border: '1.2px solid var(--ink)' }} />
          </span>
        </div>
      </div>

      {/* Hero: small-multiples — addresses the "different orders of magnitude" issue */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>small multiples · independent y-axes</SkTag>
          <SkSmallMultiples height={110} panels={[
            { label: 'ROAD · bn', accent: accent },
            { label: 'AIR · M',    accent: SK.warn },
          ]} />
        </SkBox>
      </div>

      <div style={{ padding: '8px 14px 0', display: 'flex', gap: 6, alignItems: 'stretch' }}>
        <SkBox tight style={{ flex: 1, padding: 7 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>road</div>
          <div className="sk-num" style={{ fontSize: 15 }}>4.1 bn</div>
        </SkBox>
        <SkBox tight style={{ flex: 1, padding: 7 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>air</div>
          <div className="sk-num" style={{ fontSize: 15 }}>57.8 M</div>
        </SkBox>
        <SkBox tight style={{ flex: 1, padding: 7, background: 'rgba(176,106,58,0.06)', borderStyle: 'dashed' }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.warn }}>sea</div>
          <div className="sk-num" style={{ fontSize: 13, color: SK.warn }}>n/a</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 8, color: SK.muted }}>port-based</div>
        </SkBox>
      </div>

      <CountryChips countries={['DE', 'FR', 'PL', 'EU27']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 6 · TOURISM ────────────────────────────── */
function ScreenFeatTourism({ platform }) {
  const accent = MOD_META.Tourism.accent;
  return (
    <FeatFrame platform={platform} name="Tourism" dataset="tour_occ_ninat · +1">
      <ModuleHeadline name="Tourism" value="31.7" unit="M" sub="foreign nights · accommodation" accent={accent} />

      {/* Switcher: residence chip row */}
      <div style={{ padding: '10px 14px 0' }}>
        <SkChipRow items={['Domestic', 'Foreign', 'Total']} active="Foreign" accent={accent} />
      </div>

      {/* Hero: stacked bars per year */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>nights · stacked by residence</SkTag>
          <SkStackedBars height={104} accent={accent} />
          <div style={{ display: 'flex', gap: 10, marginTop: 4, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>
            <span><span className="sk-dot" style={{ background: accent, marginRight: 4 }} />domestic</span>
            <span><span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 5, border: '1.5px solid var(--ink)', background: 'rgba(176,106,58,0.3)', verticalAlign: 'middle', marginRight: 4 }} />foreign</span>
          </div>
        </SkBox>
      </div>

      {/* Secondary: seasonality heatmap — months × last 4 years */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox tight style={{ padding: 8 }}>
          <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginBottom: 4 }}>seasonality · months × yrs</div>
          <SkHeatmap rows={4} cols={12} />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--mono)', fontSize: 8, color: SK.muted, marginTop: 2 }}>
            <span>jan</span><span>jul</span><span>dec</span>
          </div>
        </SkBox>
      </div>

      <CountryChips countries={['DE', 'FR', 'IT', 'ES']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 7 · SOCIAL ─────────────────────────────── */
function ScreenFeatSocial({ platform }) {
  const accent = MOD_META.Social.accent;
  return (
    <FeatFrame platform={platform} name="Social" tab="People" dataset="ilc_li02 · +2">
      <ModuleHeadline name="Social" value="15.0" unit="%" sub="at-risk-of-poverty · 2023" accent={accent} />

      {/* Switcher: KPI tiles — tap one to highlight its line on chart */}
      <div style={{ padding: '10px 14px 0' }}>
        <SkTileSelector active="poverty" accent={accent} items={[
          { id: 'poverty',  label: 'poverty',   value: '15.0', unit: '% · ilc_li02' },
          { id: 'atrisk',   label: 'at-risk',   value: '20.4', unit: '% · peps01' },
          { id: 'health',   label: 'health ❉',  value: '19.7', unit: '% · silc_01' },
        ]} />
      </div>

      {/* Hero: 3-line overlay, highlighted is poverty */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 8 }}>
          <SkTag>three % series · highlighted = poverty</SkTag>
          <SkMultiLineHighlighted height={120} highlightIdx={0} />
          <div style={{ display: 'flex', gap: 8, marginTop: 4, fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted, flexWrap: 'wrap' }}>
            <span style={{ color: accent }}>● poverty</span>
            <span>○ at-risk</span>
            <span>○ health</span>
          </div>
        </SkBox>
      </div>

      <YearScrubber from={2010} to={2023} />
      <CountryChips countries={['DE', 'FR', 'PL', 'EU27']} active="DE" />
    </FeatFrame>
  );
}

/* ─────────────────────────── 8 · SCIENCE ────────────────────────────── */
function ScreenFeatScience({ platform }) {
  const accent = MOD_META.Science.accent;
  return (
    <FeatFrame platform={platform} name="Science" dataset="rd_e_gerdtot · +2">
      <ModuleHeadline name="Science" value="3.09" unit="%" sub="R&D spend · % of GDP · 2023" accent={accent} />

      {/* No switcher here — radar shows ALL THREE metrics at once. */}
      <div style={{ padding: '8px 14px 0' }}>
        <SkBox style={{ padding: 6 }}>
          <SkTag>radar · all 3 % metrics · 2 countries</SkTag>
          <SkRadar size={200} metrics={['R&D %GDP', 'Internet %', 'Tertiary %']} series={[
            { color: accent,        vals: [0.62, 0.95, 0.62] },     // DE
            { color: '#b06a3a',     vals: [0.45, 0.86, 0.78] },     // FR
          ]} />
          <div style={{ display: 'flex', justifyContent: 'center', gap: 14, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginTop: -4 }}>
            <span><span className="sk-dot" style={{ background: accent, marginRight: 4 }} />DE</span>
            <span><span className="sk-dot" style={{ background: '#b06a3a', marginRight: 4 }} />FR</span>
          </div>
        </SkBox>
      </div>

      {/* Three sparklines stacked — same metrics, time-series */}
      <div style={{ padding: '8px 14px 0', display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 6 }}>
        {[
          { l: 'R&D',       v: '3.09', u: '%GDP' },
          { l: 'Internet',  v: '95',   u: '%ind' },
          { l: 'Tertiary',  v: '31.2', u: '%25-64' },
        ].map(k => (
          <SkBox tight key={k.l} style={{ padding: 6 }}>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 10, color: SK.muted, lineHeight: 1 }}>{k.l}</div>
            <div className="sk-num" style={{ fontSize: 13 }}>{k.v}<span style={{ fontSize: 9, color: SK.muted, marginLeft: 2 }}>{k.u}</span></div>
            <SkLineChart height={28} hideAxis accent={accent} />
          </SkBox>
        ))}
      </div>

      <CountryChips countries={['DE', 'FR', 'PL']} active="DE" />
    </FeatFrame>
  );
}

Object.assign(window, {
  FeatFrame,
  ScreenFeatPopulation, ScreenFeatEconomy, ScreenFeatEnvironment, ScreenFeatTrade,
  ScreenFeatTransport, ScreenFeatTourism, ScreenFeatSocial, ScreenFeatScience,
});
