/* eslint-disable */
// Wide "Anatomy of {Module}" annotation sheets — the portfolio-density artboards.
// Each sheet documents one module: data model, currently-shown vs hidden metrics,
// switcher pattern chosen, alt-chart explorations, design rationale.

function MS_Header({ name, n, switcher, hero }) {
  const accent = MOD_META[name].accent;
  return (
    <div style={{ padding: '14px 18px 10px', borderBottom: '1.5px dashed var(--ink)' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 10 }}>
        <div style={{ fontFamily: 'var(--mono)', fontSize: 11, color: SK.muted }}>0{n}</div>
        <div className="sk-wave" style={{ fontFamily: 'var(--marker)', fontSize: 30, lineHeight: 1, display: 'inline-block' }}>anatomy of {name.toLowerCase()}</div>
        <div style={{ flex: 1 }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 56, height: 22, background: accent, border: '1.5px solid var(--ink)', borderRadius: 4 }} />
          <span style={{ fontFamily: 'var(--mono)', fontSize: 11, color: SK.muted }}>{accent}</span>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 14, marginTop: 8, fontFamily: 'var(--hand)', fontSize: 13, color: SK.muted }}>
        <span>{MOD_META[name].tagline}</span>
        <span style={{ color: SK.muted2 }}>·</span>
        <span><strong style={{ color: SK.ink, fontWeight: 400 }}>hero:</strong> {hero}</span>
        <span style={{ color: SK.muted2 }}>·</span>
        <span><strong style={{ color: SK.ink, fontWeight: 400 }}>switcher:</strong> {switcher}</span>
      </div>
    </div>
  );
}

function MS_DataModel({ name, code, datasets }) {
  return (
    <SkBox style={{ padding: 12, height: '100%', boxSizing: 'border-box' }}>
      <SkTag>data model</SkTag>
      <pre style={{
        margin: 0, marginTop: 2,
        fontFamily: 'var(--mono)', fontSize: 10.5, lineHeight: 1.5,
        color: SK.ink, whiteSpace: 'pre-wrap',
        background: 'rgba(31,29,26,0.04)',
        padding: 8, borderRadius: 6,
      }}>{code}</pre>
      <div style={{ marginTop: 10 }}>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, marginBottom: 4 }}>datasets ({datasets.length})</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {datasets.map(d => (
            <div key={d.id} style={{ display: 'flex', alignItems: 'baseline', gap: 6, fontFamily: 'var(--mono)', fontSize: 10 }}>
              <span style={{ color: SK.accent }}>{d.id}</span>
              <span style={{ color: SK.muted, flex: 1 }}>{d.desc}</span>
              <span style={{ color: SK.muted2 }}>{d.unit}</span>
            </div>
          ))}
        </div>
      </div>
    </SkBox>
  );
}

function MS_Plotted({ shown, hidden, accent }) {
  return (
    <SkBox style={{ padding: 12, height: '100%', boxSizing: 'border-box' }}>
      <SkTag>plotted today · vs hidden</SkTag>

      <div style={{ marginTop: 4, padding: 8, border: `1.5px solid ${accent}`, borderRadius: 8, background: SK.paper }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted }}>
          <SkIcon name="check" size={12} stroke={accent} />on chart now
        </div>
        <div className="sk-num" style={{ fontSize: 18, color: accent, marginTop: 2 }}>{shown.value}</div>
        <div style={{ fontFamily: 'var(--hand)', fontSize: 12 }}>{shown.field}</div>
        <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted, marginTop: 2 }}>{shown.unit}</div>
      </div>

      <div style={{ marginTop: 10, fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>available but hidden:</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 4 }}>
        {hidden.map(h => (
          <div key={h.field} style={{
            padding: '6px 8px',
            border: '1.5px dashed var(--ink)',
            borderRadius: 8,
            background: SK.paper,
            opacity: 0.85,
          }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
              <span className="sk-num" style={{ fontSize: 14 }}>{h.value}</span>
              <span style={{ fontFamily: 'var(--hand)', fontSize: 11 }}>{h.field}</span>
              <span style={{ flex: 1 }} />
              <span style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted }}>{h.unit}</span>
            </div>
            {h.note && <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, marginTop: 1 }}>{h.note}</div>}
          </div>
        ))}
      </div>
    </SkBox>
  );
}

function MS_Switcher({ pattern, why, accent, preview }) {
  return (
    <SkBox style={{ padding: 12, height: '100%', boxSizing: 'border-box' }}>
      <SkTag>switcher · {pattern}</SkTag>
      <div style={{ marginTop: 4 }}>{preview}</div>
      <div style={{ marginTop: 10, fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, lineHeight: 1.35 }}>
        <strong style={{ color: SK.ink, fontWeight: 400, textDecoration: `underline wavy ${accent} 1.2px`, textUnderlineOffset: 3 }}>why this</strong>
        <div style={{ marginTop: 4 }}>{why}</div>
      </div>
    </SkBox>
  );
}

function MS_AltCharts({ alts }) {
  return (
    <SkBox style={{ padding: 12, height: '100%', boxSizing: 'border-box' }}>
      <SkTag>alt chart explorations</SkTag>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginTop: 4, height: 'calc(100% - 20px)' }}>
        {alts.map((a, i) => (
          <div key={i} style={{ position: 'relative', display: 'flex', flexDirection: 'column' }}>
            <div style={{ flex: 1, position: 'relative' }}>{a.viz}</div>
            <div style={{ fontFamily: 'var(--marker)', fontSize: 13, marginTop: 4, color: SK.ink, lineHeight: 1 }}>{a.title}</div>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 10.5, color: SK.muted, marginTop: 1, lineHeight: 1.25 }}>{a.note}</div>
          </div>
        ))}
      </div>
    </SkBox>
  );
}

// Master sheet renderer.
function ModuleSheet({ name, n, switcher, hero, code, datasets, shown, hidden, switcherWhy, switcherPreview, alts }) {
  return (
    <div className="sk sk-paper" style={{ height: '100%', boxSizing: 'border-box', display: 'flex', flexDirection: 'column' }}>
      <MS_Header name={name} n={n} switcher={switcher} hero={hero} />
      <div style={{ flex: 1, display: 'grid', gridTemplateRows: '1.05fr 1fr', gap: 10, padding: 12, minHeight: 0 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.15fr 0.9fr 1.1fr', gap: 10, minHeight: 0 }}>
          <MS_DataModel name={name} code={code} datasets={datasets} />
          <MS_Plotted shown={shown} hidden={hidden} accent={MOD_META[name].accent} />
          <MS_Switcher pattern={switcher} why={switcherWhy} accent={MOD_META[name].accent} preview={switcherPreview} />
        </div>
        <MS_AltCharts alts={alts} />
      </div>
    </div>
  );
}

/* ───────────────────────── individual sheets ─────────────────────────── */

function SheetPopulation() {
  const accent = MOD_META.Population.accent;
  return <ModuleSheet
    name="Population" n="1"
    hero="age-pyramid · single year"
    switcher="3-state segmented"
    code={`data class Pop(
  countryCode: String,    // "DE"
  year: Int,              // 2024
  totalPop: Long,         // 83_166_711
  malePop: Long?,         // 40_900_000
  femalePop: Long?,       // 42_266_711
)`}
    datasets={[{ id: 'demo_pjan', desc: 'population by sex × age × geo × time', unit: 'people' }]}
    shown={{ value: '83.2M', field: 'totalPopulation', unit: 'people · sum across ages' }}
    hidden={[
      { value: '40.9M', field: 'malePopulation',   unit: 'sex=M',     note: 'shown only as pyramid left wing' },
      { value: '42.3M', field: 'femalePopulation', unit: 'sex=F',     note: 'pyramid right wing' },
      { value: '~22',   field: 'cohorts(5-yr)',    unit: 'age groups', note: 'enables pyramid hero' },
    ]}
    switcherWhy="three mutually-exclusive views (Total / Men / Women) — a segmented control is the cleanest Compose pattern. Pyramid stays visible always; toggle just shifts the highlighted side."
    switcherPreview={<SkSegmented items={['Total', 'Men', 'Women']} active="Total" accent={accent} />}
    alts={[
      { title: 'pyramid', note: 'chosen · shows sex+age in one viz', viz: <div style={{ position: 'relative' }}><SkPyramid height={120} /></div> },
      { title: 'two lines', note: 'total over time + breakdown', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: 'stacked area', note: 'm/f over years', viz: <SkArea height={120} /> },
      { title: 'cohort heatmap', note: 'age × year', viz: <div style={{ paddingTop: 6 }}><SkHeatmap rows={5} cols={10} /></div> },
    ]}
  />;
}

function SheetEconomy() {
  const accent = MOD_META.Economy.accent;
  return <ModuleSheet
    name="Economy" n="2"
    hero="multi-country GDP line"
    switcher="3-way segmented"
    code={`data class Econ(
  countryCode: String,
  year: Int,
  unit: EconomyUnit,
  gdpEur: Long?,         // 3_450_720 (M€)
  hicpIndex: Double?,    // 105.8
  deficitPctGdp: Double? // -2.5
)`}
    datasets={[
      { id: 'nama_10_gdp',     desc: 'GDP · current prices',     unit: 'M€' },
      { id: 'prc_hicp_aind',   desc: 'HICP inflation index',     unit: 'idx' },
      { id: 'gov_10dd_edpt1',  desc: 'government deficit',       unit: '%GDP' },
    ]}
    shown={{ value: '3 451 B€', field: 'gdpEur', unit: 'current prices · M€ ×1000' }}
    hidden={[
      { value: '105.8',  field: 'hicpIndex',      unit: 'idx · 2015=100',     note: 'inflation — completely different axis' },
      { value: '−2.5%',  field: 'deficitPctGdp',  unit: '% of GDP',           note: 'can be negative — needs zero line' },
    ]}
    switcherWhy="three macro metrics with wildly different scales (B€ / index / %) can't share an axis. A segmented control swaps the whole chart — clearer than dual-axis hacks."
    switcherPreview={<SkSegmented items={['GDP', 'Inflation', 'Deficit']} active="GDP" accent={accent} />}
    alts={[
      { title: 'segmented (chosen)', note: 'one chart per metric', viz: <SkLineChart height={120} lines={3} accent={accent} hideAxis /> },
      { title: 'mini-dashboard', note: 'all 3 stacked small', viz: <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}><SkLineChart height={36} hideAxis accent={accent} /><SkLineChart height={36} hideAxis accent={SK.warn} /><SkLineChart height={36} hideAxis accent="#5e6b58" /></div> },
      { title: 'dual axis', note: 'GDP + inflation on one', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: 'bars · % chg', note: 'normalize to YoY %', viz: <SkBars n={8} height={120} accent={accent} /> },
    ]}
  />;
}

function SheetEnvironment() {
  const accent = MOD_META.Environment.accent;
  return <ModuleSheet
    name="Environment" n="3"
    hero="GHG line · sector-aware"
    switcher="chips + dropdown"
    code={`data class Env(
  countryCode: String,
  year: Int,
  sector: EnvSector,           // TOTAL | TRANSPORT | INDUSTRY
  ghgEmissions: Double?,       // Mt CO₂-eq
  energyConsumption: Double?,  // ktoe
  sdg13Score: Double?,         // idx · 1990=100
)`}
    datasets={[
      { id: 'env_air_gge', desc: 'GHG emissions',         unit: 'Mt CO₂-eq' },
      { id: 'nrg_bal_c',   desc: 'energy consumption',    unit: 'ktoe' },
      { id: 'sdg_13_10',   desc: 'SDG13 climate action',  unit: 'idx' },
    ]}
    shown={{ value: '733 Mt', field: 'ghgEmissions · TOTAL', unit: 'million tonnes CO₂-eq' }}
    hidden={[
      { value: '194 248', field: 'energyConsumption', unit: 'ktoe',         note: 'similar shape, different units' },
      { value: '58.3',    field: 'sdg13Score',        unit: 'idx 1990=100', note: 'normalised — easy to compare countries' },
      { value: '3',       field: 'sector enum',       unit: 'TOT/TRA/IND',  note: 'currently filter, could be small-mult' },
    ]}
    switcherWhy="two orthogonal axes — sector (TOTAL/TRANSPORT/INDUSTRY) and metric (GHG/Energy/SDG). Chips for sector (3 short options), dropdown for metric (3 long ones, more likely to grow)."
    switcherPreview={<div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}><SkDropdown label="metric:" value="GHG" /><SkChipRow items={['TOTAL', 'TRANSPORT', 'INDUSTRY']} active="TOTAL" accent={accent} /></div>}
    alts={[
      { title: 'sector overlay', note: 'chosen · one country, 3 sectors as series', viz: <SkLineChart height={120} lines={3} accent={accent} accent2={SK.warn} accent3="#7d5e76" hideAxis /> },
      { title: 'split panels', note: 'one sector per panel', viz: <SkSmallMultiples height={120} panels={[{ label: 'TOT', accent }, { label: 'TRA', accent: SK.warn }]} /> },
      { title: 'stacked area', note: 'sectors sum to total', viz: <SkArea height={120} /> },
      { title: 'sdg gauge', note: 'index alt view', viz: <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 120 }}><SkDonut size={100} label="58" /></div> },
    ]}
  />;
}

function SheetTrade() {
  const accent = MOD_META.Trade.accent;
  return <ModuleSheet
    name="Trade" n="4"
    hero="diverging bars (imports vs exports)"
    switcher="underlined tabs"
    code={`data class Trade(
  countryCode: String,
  year: Int,
  partner: String,           // "EU27_2020"
  exportsEur: Long?,         // M€ out
  importsEur: Long?,         // M€ in
  balanceEur: Long?,         // exp − imp (signed)
)`}
    datasets={[{ id: 'ext_lt_intratrd', desc: 'intra-EU trade by SITC product', unit: 'M€' }]}
    shown={{ value: '1 612 B€', field: 'exportsEur', unit: 'millions EUR · DE → EU27' }}
    hidden={[
      { value: '1 523 B€', field: 'importsEur',  unit: 'M€',           note: 'natural pair with exports' },
      { value: '+89 B€',   field: 'balanceEur',  unit: 'M€ · signed',  note: 'can be negative — needs zero line' },
      { value: '·',        field: 'partner dim', unit: 'EU27 | WORLD', note: 'currently hardcoded to EU27' },
    ]}
    switcherWhy="three views of the same flows: directional (Exp/Imp) and a derived signed view (Balance). Tabs read as 'modes of the same dataset' — segmented would feel like unrelated metrics."
    switcherPreview={<SkTabs items={['Exports', 'Imports', 'Balance']} active="Balance" accent={accent} />}
    alts={[
      { title: 'diverging bars', note: 'chosen · exp ↑ imp ↓', viz: <SkDivergingBars height={120} accent={accent} warn={SK.warn} /> },
      { title: 'sankey · partners', note: 'flow to top destinations', viz: <SkSankey height={120} /> },
      { title: 'balance bars', note: 'signed bars, zero-line', viz: <SkBars n={8} height={120} accent={accent} /> },
      { title: 'two lines', note: 'exp + imp overlay', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
    ]}
  />;
}

function SheetTransport() {
  const accent = MOD_META.Transport.accent;
  return <ModuleSheet
    name="Transport" n="5"
    hero="small-multiples · independent y-axes"
    switcher="pill toggle + log switch"
    code={`data class Trans(
  countryCode: String,
  year: Int,
  mode: TransportMode,         // ROAD | AIR | SEA | ALL
  roadPassengers: Long?,       // 4_108_700_000  (×1000 scale)
  airPassengers:  Long?,       //    57_795_978
  seaPassengers:  Long?,       //  always null — disabled
)`}
    datasets={[
      { id: 'road_pa_buscoa', desc: 'road bus passengers', unit: 'pax (×1000)' },
      { id: 'avia_paoc',      desc: 'air passengers · carried', unit: 'pax' },
      { id: 'mar_pa_aa',      desc: 'sea — disabled, port-dim', unit: '—' },
    ]}
    shown={{ value: '4.1 bn', field: 'roadPassengers (or air if null)', unit: 'pax · raw count' }}
    hidden={[
      { value: '57.8 M', field: 'airPassengers',  unit: 'pax', note: '~100× smaller than road · invisible on shared y' },
      { value: '—',      field: 'seaPassengers',  unit: '—',   note: 'API uses c_port (not geo) — needs rework' },
    ]}
    switcherWhy="ROAD and AIR differ by 2 orders of magnitude. A single chart hides small countries. Small-multiples solves it with parallel axes; pill toggle lets users still focus one mode."
    switcherPreview={<SkPillToggle items={['ROAD', 'AIR', 'ALL']} active="ALL" accent={accent} />}
    alts={[
      { title: 'small-multiples', note: 'chosen · independent y', viz: <SkSmallMultiples height={120} panels={[{ label: 'ROAD', accent }, { label: 'AIR', accent: SK.warn }]} /> },
      { title: 'log scale', note: 'same chart, log y', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: 'normalised', note: 'index to base year', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: 'mode share', note: 'donut · road/air/other', viz: <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 120 }}><SkDonut size={100} label="84/16" /></div> },
    ]}
  />;
}

function SheetTourism() {
  const accent = MOD_META.Tourism.accent;
  return <ModuleSheet
    name="Tourism" n="6"
    hero="stacked bars · domestic vs foreign"
    switcher="chip row (residence)"
    code={`data class Tour(
  countryCode: String,
  year: Int,
  residence: TourismResidence, // DOM | FOR | ALL
  nights: Long,                // 31_732_508
  trips: Long?,                // rarely populated
)`}
    datasets={[
      { id: 'tour_occ_ninat', desc: 'nights at accommodation · by residence', unit: 'nights' },
      { id: 'tour_dem_tttot', desc: 'trips · total · no residence dim',       unit: 'trips' },
    ]}
    shown={{ value: '31.7 M', field: 'nights · FOR', unit: 'foreign-visitor nights' }}
    hidden={[
      { value: '~95 M', field: 'nights · DOM',      unit: 'domestic',  note: 'natural stacking pair with foreign' },
      { value: 'sparse', field: 'trips',            unit: 'trips count', note: 'most cells null — show as KPI not series' },
      { value: '12×N',   field: 'monthly seasonality', unit: 'months', note: 'underused — heatmap candidate' },
    ]}
    switcherWhy="DOM/FOR/ALL are categories of the same value, not separate KPIs — chips read as filters. Stacked bars by default so you always see the relationship; the chip just shifts emphasis."
    switcherPreview={<SkChipRow items={['Domestic', 'Foreign', 'Total']} active="Foreign" accent={accent} />}
    alts={[
      { title: 'stacked bars', note: 'chosen · dom + for sum to total', viz: <SkStackedBars height={120} accent={accent} /> },
      { title: 'season heat', note: 'months × years', viz: <div style={{ paddingTop: 6 }}><SkHeatmap rows={5} cols={12} /></div> },
      { title: 'two lines', note: 'dom & for trends', viz: <SkLineChart height={120} lines={2} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: '%-share', note: 'foreign-share line', viz: <SkLineChart height={120} accent={accent} hideAxis /> },
    ]}
  />;
}

function SheetSocial() {
  const accent = MOD_META.Social.accent;
  return <ModuleSheet
    name="Social" n="7"
    hero="three % lines · KPI-tile-driven highlight"
    switcher="3-up KPI tile selector"
    code={`data class Soc(
  countryCode: String,
  year: Int,
  povertyRate: Double?,        // 15.0 %
  atRiskRate: Double?,         // 20.4 %
  healthSatisfaction: Double?, // 19.7 %  ("very good")
)`}
    datasets={[
      { id: 'ilc_li02',     desc: 'at-risk-of-poverty rate',         unit: '%' },
      { id: 'ilc_peps01',   desc: 'poverty-or-social-exclusion',     unit: '%' },
      { id: 'hlth_silc_01', desc: 'self-perceived "very good" health', unit: '%' },
    ]}
    shown={{ value: '15.0 %', field: 'povertyRate', unit: 'at-risk-of-poverty share' }}
    hidden={[
      { value: '20.4 %', field: 'atRiskRate',         unit: 'PC · EU 2020 target', note: 'shares axis with poverty — overlap natural' },
      { value: '19.7 %', field: 'healthSatisfaction', unit: 'PC · "very good"',     note: 'related but inverse direction' },
    ]}
    switcherWhy="all 3 metrics live on % axis — they CAN overlay. KPI tiles double as the switcher: tapping a tile pops its line forward, others dim. The tile shows the current value, so the switcher itself is informative."
    switcherPreview={<SkTileSelector active="poverty" accent={accent} items={[{ id: 'poverty', label: 'poverty', value: '15.0', unit: '%' }, { id: 'atrisk', label: 'at-risk', value: '20.4', unit: '%' }, { id: 'health', label: 'health', value: '19.7', unit: '%' }]} />}
    alts={[
      { title: '3-line · highlight', note: 'chosen', viz: <SkMultiLineHighlighted height={120} highlightIdx={0} /> },
      { title: 'small-multiples', note: 'three side-by-side', viz: <SkSmallMultiples height={120} panels={[{ label: 'pov', accent }, { label: 'risk', accent: SK.warn }]} /> },
      { title: 'radar', note: 'snapshot vs EU avg', viz: <div style={{ paddingTop: 0 }}><SkRadar size={130} metrics={['pov', 'risk', 'health']} series={[{ color: accent, vals: [0.5, 0.7, 0.55] }]} /></div> },
      { title: 'rank slope', note: 'country rank shift', viz: <SkLineChart height={120} lines={3} accent={accent} hideAxis /> },
    ]}
  />;
}

function SheetScience() {
  const accent = MOD_META.Science.accent;
  return <ModuleSheet
    name="Science" n="8"
    hero="radar · 3 % metrics, 2-3 countries"
    switcher="none — radar shows all"
    code={`data class Sci(
  countryCode: String,
  year: Int,
  rdSpendPctGdp: Double?,    // 3.09 %
  internetUsagePct: Double?, // 95 %
  tertiaryEducPct: Double?,  // 31.2 %
)`}
    datasets={[
      { id: 'rd_e_gerdtot',    desc: 'R&D spending',         unit: '% of GDP' },
      { id: 'isoc_ci_ifp_iu',  desc: 'internet usage 3mo',   unit: '% of indiv' },
      { id: 'edat_lfse_03',    desc: 'tertiary educ 25-64',  unit: '%' },
    ]}
    shown={{ value: '3.09 %', field: 'rdSpendPctGdp', unit: 'gross R&D spend · share of GDP' }}
    hidden={[
      { value: '95 %',   field: 'internetUsagePct', unit: '% individuals', note: 'high · little variance across yrs' },
      { value: '31.2 %', field: 'tertiaryEducPct',  unit: '% age 25-64',   note: 'slow-moving · good radar axis' },
    ]}
    switcherWhy="all 3 metrics are % — same axis, same year-cadence, different domains. Radar shows the country's 'shape' at a glance — no switcher needed. Sparklines below recover the time-dimension."
    switcherPreview={<div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 4 }}><div style={{ fontFamily: 'var(--marker)', fontSize: 22, color: SK.muted }}>—  no switcher  —</div></div>}
    alts={[
      { title: 'radar (chosen)', note: 'all 3 at once', viz: <div style={{ marginTop: -4 }}><SkRadar size={130} metrics={['R&D', 'Net', 'Edu']} series={[{ color: accent, vals: [0.62, 0.95, 0.62] }, { color: SK.warn, vals: [0.45, 0.86, 0.78] }]} /></div> },
      { title: '3 sparklines', note: 'time per metric', viz: <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}><SkLineChart height={36} hideAxis accent={accent} /><SkLineChart height={36} hideAxis accent={SK.warn} /><SkLineChart height={36} hideAxis accent="#5e6b58" /></div> },
      { title: 'parallel coords', note: 'each country = polyline', viz: <SkLineChart height={120} lines={3} accent={accent} accent2={SK.warn} hideAxis /> },
      { title: 'dot-plot', note: 'country dots on 3 axes', viz: <SkBars n={6} height={120} horizontal accent={accent} /> },
    ]}
  />;
}

Object.assign(window, {
  ModuleSheet,
  SheetPopulation, SheetEconomy, SheetEnvironment, SheetTrade,
  SheetTransport, SheetTourism, SheetSocial, SheetScience,
});
