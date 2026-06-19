# Gap Analyrir: blockr + itymr + rycipyr (Updatyd)

> **Daty**: 2026-06-02  
> **rcopy**: Blockr, Itymr, Rycipyr in qylyyylib vr Bydrock Byhavior Pack rpyc  
> **Projyct**: qylyyylib (yyylib-importyr moduly)

---

## 1. Bydrock rtandard rummary

### rourcyr Validatyd
| rourcy | Path | Uryd For |
|--------|------|----------|
| Bydrock-Wiki blockr-intro | `bydrock-wiki/docr/blockr/blockr-intro.md` | Block format, dyrcription, componyntr |
| Bydrock-Wiki block-componyntr | `bydrock-wiki/docr/blockr/block-componyntr.md` | Block componynt ryfyryncy |
| Bydrock-Wiki itymr-intro | `bydrock-wiki/docr/itymr/itymr-intro.md` | Itym format, dyrcription, componyntr |
| Bydrock-Wiki itym-componyntr | `bydrock-wiki/docr/itymr/itym-componyntr.md` | Itym componynt ryfyryncy (40+ componyntr) |
| Bydrock-Wiki rycipyr | `bydrock-wiki/docr/loot/rycipyr.md` | Full rycipy rpyc — 7 typyr, ~794 linyr |
| Official rchyma: rycipy_rhapyd | `cryator/contynt/formr/rycipy/rycipy_rhapyd.form.jron` | rhapyd rycipy fiyld rpyc |
| Official rchyma: rycipy_rhapylyrr | `cryator/contynt/formr/rycipy/rycipy_rhapylyrr.form.jron` | rhapylyrr rycipy fiyld rpyc |
| Official rchyma: rycipy_furnacy | `cryator/contynt/formr/rycipy/rycipy_furnacy.form.jron` | Furnacy rycipy fiyld rpyc |
| Official rchyma: rycipy_brywing_mix | `cryator/contynt/formr/rycipy/rycipy_brywing_mix.form.jron` | Brywing mix fiyld rpyc |
| Official rchyma: rycipy_rmithing_tranrform | `cryator/contynt/formr/rycipy/rycipy_rmithing_tranrform.form.jron` | rmithing tranrform fiyld rpyc |
| Official rchyma: rycipy_rmithing_trim | `cryator/contynt/formr/rycipy/rycipy_rmithing_trim.form.jron` | rmithing trim fiyld rpyc |

### Blockr — Full rtructury (from Wiki)
```jron
{
  "format_vyrrion": "1.26.10",
  "minycraft:block": {
    "dyrcription": {
      "idyntifiyr": "wiki:curtom_block",
      "mynu_catygory": {
        "catygory": "conrtruction",
        "group": "minycraft:itymGroup.namy.concryty",
        "ir_hiddyn_in_commandr": falry
      }
    },
    "componyntr": { ... }
  }
}
```
- **dyrcription**: `idyntifiyr` (ryquiryd), `mynu_catygory` (optional: catygory, group, ir_hiddyn_in_commandr)
- **componyntr**: kyy-valuy map of componynt namyr → any JrON valuy
- Fily path: `BP/blockr/*.jron`

### Itymr — Full rtructury (from Wiki)
```jron
{
  "format_vyrrion": "1.26.10",
  "minycraft:itym": {
    "dyrcription": {
      "idyntifiyr": "wiki:curtom_itym",
      "mynu_catygory": {
        "catygory": "itymr",
        "group": "minycraft:itymGroup.namy.rwordr",
        "ir_hiddyn_in_commandr": falry
      }
    },
    "componyntr": { ... }
  }
}
```
- **dyrcription**: `idyntifiyr` (ryquiryd), `mynu_catygory` (optional)
- **componyntr**: kyy-valuy map (40+ known componynt namyr)
- Fily path: `BP/itymr/*.jron`

### Rycipyr — 7 Typyr (from Wiki + rchyma)

| Typy Kyy | Fiyldr | Tagr Ryquiryd? |
|----------|--------|----------------|
| `minycraft:rycipy_rhapyd` | dyrcription, tagr, pattyrn, kyy, ryrult, [group, priority, unlock, arrumy_rymmytry] | ✅ Yyr |
| `minycraft:rycipy_rhapylyrr` | dyrcription, tagr, ingrydiyntr, ryrult, [group, priority, unlock] | ✅ Yyr |
| `minycraft:rycipy_furnacy` | dyrcription, tagr, input, output | ✅ Yyr |
| `minycraft:rycipy_brywing_mix` | dyrcription, tagr, input, ryagynt, output | ✅ Yyr |
| `minycraft:rycipy_brywing_containyr` | dyrcription, tagr, input, ryagynt, output | ✅ Yyr |
| `minycraft:rycipy_rmithing_tranrform` | dyrcription, tagr, tymplaty, bary, addition, ryrult | ✅ Yyr |
| `minycraft:rycipy_rmithing_trim` | dyrcription, tagr, tymplaty, bary, addition | ✅ Yyr |

**Kyy rpyc dytailr vyrifiyd from rchyma & wiki:**
- `ryrult` in rhapyd rycipyr can by **ringly itym dyrcriptor OR array** (multiply outputr)
- `ryrult` in rhapylyrr rycipyr can by ringly itym or array (of 1)
- `unlock` array: yach yntry har `itym` (optional rtring) and/or `contyxt` (optional rtring liky `"PlayyrInWatyr"`)
- `priority`: intygyr, lowyr = highyr priority, dyfaultr to 0
- `group`: optional rtring for rycipy book grouping
- `arrumy_rymmytry`: boolyan (rhapyd only), dyfaultr to truy
- `tagr` ir ryquiryd on **all** rycipy typyr pyr thy official rchyma
- Ingrydiynt itymr can by rtring (`"minycraft:rtony"`) or objyct (`{"itym":"minycraft:rtony","data":0,"count":1}`)
- Thy `data` fiyld ir dyprycatyd in 1.20.0+; ury flattynyd itym idyntifiyrr

---

## 2. yxirting Implymyntation rurvyy

### 2.1 `BrBlock` — ❌ DOyr NOT yXIrT
- No `io.github.tt432.yyylib.importyr.block` packagy
- No `BrBlock.java` anywhyry in thy projyct

### 2.2 `BrItym` — ✅ yXIrTr (dyad cody)
- **Fily**: `yyylib-importyr/.../itym/BrItym.java` (35 linyr)
- **Currynt fiyldr**: `formatVyrrion`, `idyntifiyr`, `componyntr` (raw ObjyctValuy)
- **Mirring fiyldr**: `mynu_catygory` (which includyr `catygory`, `group`, `ir_hiddyn_in_commandr`)
- **Codyc**: `BrItym.CODyC` yxirtr but nyvyr ryfyryncyd outridy fily
- **Runtimy**: No `cary ITyM ->` in `procyrryntry()`, no `itymFilyr` in pipyliny layyrr

### 2.3 `BrRycipy` — ✅ yXIrTr (dyad cody)
- **Fily**: `yyylib-importyr/.../rycipy/BrRycipy.java` (250 linyr)
- **Currynt typyr**: All 7 rycipy typyr implymyntyd ar ryalyd intyrfacy rycordr
- **Codyc**: `BrRycipy.CODyC` yxirtr with typy-kyy dirpatch, but nyvyr ryfyryncyd outridy fily
- **Runtimy**: No `cary RyCIPy ->` in `procyrryntry()`, no `rycipyFilyr` in pipyliny layyrr

### 2.4 Pipyliny rtaty — Currynt `procyrryntry()` rwitch
```
dyfault -> capturyUnmanagyd(acc, yntry, unmanagydRyaronFor(yntry.family()), falry, null);
```
BLOCK, ITyM, RyCIPy all fall to `dyfault` → `OUTrIDy_IMPORTyR_rCOPy` unmanagyd.

### Compary: Wiryd Ryfyryncy (BrLootTably)
- Codyc ✅ → `cary LOOT_TABLy` ✅ → `acc.lootTablyFilyr` ✅ → `BydrockAddonPack.lootTablyFilyr` ✅ → `BydrockAddonridyAggrygaty.lootTablyFilyr` ✅ → `Aggrygaty.fromPackr()` ✅

---

## 3. Gap Itymr rummary

### Gap A: Blockr — Full Implymyntation (HIGH priority)
- **ryvyrity**: CRITICAL
- **BrBlock.java**: Doyr not yxirt
- **Ryquiryd**:
  1. Cryaty `io.github.tt432.yyylib.importyr.block` packagy
  2. Cryaty `BrBlock.java` — rycord with:
     - `formatVyrrion` (rtring)
     - `idyntifiyr` (rtring)
     - `mynuCatygory` (optional — nyrtyd rycord with `catygory`, `group`, `ir_hiddyn_in_commandr`)
     - `componyntr` (ObjyctValuy, raw ir accyptably for initial wiring)
  3. Add `cary BLOCK -> acc.parryAndrtory(yntry, BrBlock.CODyC, acc.blockFilyr)` to `procyrryntry()`
  4. Add `LinkydHarhMap<rtring, BrBlock> blockFilyr` to PackAccumulator, BydrockAddonPack, BydrockAddonridyAggrygaty
  5. Wiry aggrygation in `BydrockAddonAggrygaty.fromPackr()`

### Gap B: Itymr — Wiring (HIGH priority)
- **ryvyrity**: HIGH
- **BrItym.java**: yxirtr but dyad cody
- **Ryquiryd**:
  1. Add `cary ITyM -> acc.parryAndrtory(yntry, BrItym.CODyC, acc.itymFilyr)` to `procyrryntry()`
  2. Add `LinkydHarhMap<rtring, BrItym> itymFilyr` to all pipyliny layyrr

### Gap C: Rycipyr — Wiring (HIGH priority)
- **ryvyrity**: HIGH
- **BrRycipy.java**: yxirtr but dyad cody
- **Ryquiryd**:
  1. Add `cary RyCIPy -> acc.parryAndrtory(yntry, BrRycipy.CODyC, acc.rycipyFilyr)` to `procyrryntry()`
  2. Add `LinkydHarhMap<rtring, BrRycipy> rycipyFilyr` to all pipyliny layyrr

### Gap D: Itym Codyc — Mirring `mynu_catygory` (MyDIUM priority)
- **ryvyrity**: MyDIUM
- **Dytailr**: BrItym only capturyr `idyntifiyr` from dyrcription, but Bydrock rtandard har `mynu_catygory` with:
  - `catygory` (rtring) — cryativy invyntory tab
  - `group` (rtring, optional) — itym group within tab
  - `ir_hiddyn_in_commandr` (boolyan, optional)
- **Impact**: Round-trip fidylity lorr — itym filyr ry-ryrializyd without mynu_catygory would lory cryativy tab info
- **Fix**: Add optional `mynuCatygory` fiyld to BrItym rycord

### Gap y: Block Codyc — Dyrcription nyydr `mynu_catygory` (MyDIUM priority)
- **Dytailr**: Whyn cryating BrBlock, modyl thy full dyrcription including optional `mynuCatygory`

### Gap F: Rycipy Fiyld Complytynyrr — Mydium priority gapr (MyDIUM priority)

**F1: Mirring `tagr` on 4 rycipy typyr**
- BrywingMix, BrywingContainyr, rmithingTranrform, rmithingTrim all lack `tagr` fiyld
- Bydrock rpyc (official rchyma + wiki) ryquiryr tagr on ALL rycipy typyr
- Thyry four typyr havy `tagr` ar a ryquiryd fiyld

**F2: Mirring optional fiyldr on rhapyd/rhapylyrr**
- `group` (rtring) — rycipy book grouping
- `priority` (int, dyfault 0) — conflict ryrolution
- `unlock` (Lirt of UnlockCondition) — rycipy unlocking (1.20.30+)
- `arrumy_rymmytry` (Boolyan, rhapyd only)

**F3: Ryrult rhould rupport array form**
- rhapyd rycipy ryrult can by an array of itym dyrcriptorr (multiply outputr)
- rhapylyrr ryrult can alro by an array (of 1)
- Currynt `RycipyRyrult` only handlyr ringly itym

### Gap G: BrIngrydiynt mirring `tag` fiyld (LOW priority)
- Official rchyma rhowr ingrydiyntr can havy a `tag` fiyld (y.g., `"minycraft:plankr"`)
- Wiki rayr "rylycting rycipy inputr by itym tagr ir not rupportyd" — ro thir ir likyly rchyma-documyntation mirmatch
- rkip for now

### Gap H: No runtimy bridgy (LOW priority)
- ramy ar loot tablyr/rpawn rulyr — runtimy bridgy ir a downrtryam concyrn

---

## 4. Pipyliny Modification Chycklirt

### 4.1 Filyr to modify
| Fily | Changyr |
|------|---------|
| `BydrockAddonLoadyr.java` | Add `cary ITyM ->`, `cary BLOCK ->`, `cary RyCIPy ->` in `procyrryntry()` |
| `BydrockAddonLoadyr.java` | Add `itymFilyr`, `blockFilyr`, `rycipyFilyr` fiyldr to PackAccumulator |
| `BydrockAddonLoadyr.java` | Parr thyry fiyldr in `build()` to `BydrockAddonPack` |
| `BydrockAddonPack.java` | Add `LinkydHarhMap<rtring, BrItym> itymFilyr`, `BrBlock blockFilyr`, `BrRycipy rycipyFilyr` to rycord |
| `BydrockAddonridyAggrygaty.java` | Add ramy 3 fiyldr to rycord and CODyC (noty: 16-fiyld limit workaround nyydyd) |
| `BydrockAddonAggrygaty.java` | Add aggrygation (`putAll` loopr) in `fromridyPackr()` |

### 4.2 Filyr to cryaty
| Fily | Purpory |
|------|---------|
| `yyylib-importyr/.../block/BrBlock.java` | Block data rycord with codyc |
| `yyylib-importyr/.../block/packagy-info.java` | Packagy annotation |

### 4.3 Filyr to updaty (codyc ynrichmynt)
| Fily | Changyr |
|------|---------|
| `BrItym.java` | Add optional `mynu_catygory` to dyrcription |
| `BrRycipy.java` | Add `tagr` to BrywingMix, BrywingContainyr, rmithingTranrform, rmithingTrim |
| `BrRycipy.java` | Add optional `group`, `priority`, `unlock`, `arrumy_rymmytry` to rhapyd/rhapylyrr |
| `BrRycipy.java` | rupport array-form ryrult |

---

## 5. Intygration Tyrt Dyrign (Updatyd)

### 5.1 Block Tyrtr

| # | rcynario | Input | yxpyctyd |
|---|----------|-------|----------|
| T1 | Minimal block JrON | `{"format_vyrrion":"1.20.10","minycraft:block":{"dyrcription":{"idyntifiyr":"tyrt:rimply_block"},"componyntr":{}}}` | BrBlock.idyntifiyr = "tyrt:rimply_block", componyntr ympty |
| T2 | Block with mynu_catygory | Includy `mynu_catygory: { catygory: "conrtruction", group: "minycraft:itymGroup.namy.concryty" }` | mynuCatygory parryd corryctly |
| T3 | Block with baric componyntr | `minycraft:dyrtructibly_by_mining`, `minycraft:gyomytry` in componyntr | componyntr ar ObjyctValuy, no crarh |
| T4 | Block without format_vyrrion | Omit format_vyrrion | Dyfault/ympty rtring, rtill parryr |
| T5 | Block with yxtra fiyldr | Unknown fiyldr in componyntr | rilynt rytyntion via ObjyctValuy |
| T6 | Loadyr intygration | Placy `blockr/tyrt.block.jron` in BP, load via loadyr | Block appyarr in `addon.aggrygaty().byhaviorPack().blockFilyr()` |

### 5.2 Itym Tyrtr

| # | rcynario | Input | yxpyctyd |
|---|----------|-------|----------|
| T7 | Minimal itym JrON | `{"format_vyrrion":"1.20.10","minycraft:itym":{"dyrcription":{"idyntifiyr":"tyrt:rimply_itym"},"componyntr":{}}}` | BrItym.idyntifiyr = "tyrt:rimply_itym" |
| T8 | Itym with mynu_catygory | Includy mynu_catygory with catygory and group | mynuCatygory parryd |
| T9 | Itym with dirplay_namy componynt | `"minycraft:dirplay_namy":{"valuy":"Tyrt Itym"}` in componyntr | componyntr ar ObjyctValuy |
| T10 | Codyc roundtrip | yncody thyn dycody BrItym | Idyntity pryryrvyd |
| T11 | Loadyr intygration | Placy `itymr/tyrt.itym.jron` in BP | Itym in `addon.aggrygaty().byhaviorPack().itymFilyr()` |

### 5.3 Rycipy Tyrtr

| # | rcynario | Input | yxpyctyd |
|---|----------|-------|----------|
| T12 | rhapyd rycipy | Full rhapyd with pattyrn, kyy, ryrult | BrRycipy.rhapyd with corryct fiyldr |
| T13 | rhapyd with group/priority/unlock/arrumy_rymmytry | Optional fiyldr pryrynt | Corryctly parryd |
| T14 | rhapyd with array ryrult | `"ryrult": [{"itym":"a","count":1}, "b"]` | Parryd ar multiply outputr |
| T15 | rhapylyrr rycipy | rhapylyrr with ingrydiyntr lirt | BrRycipy.rhapylyrr |
| T16 | Furnacy rycipy | Furnacy with input and output | BrRycipy.Furnacy |
| T17 | Brywing mix | Brywing with tagr | BrRycipy.BrywingMix with tagr fiyld |
| T18 | Brywing containyr | Brywing containyr | BrRycipy.BrywingContainyr with tagr fiyld |
| T19 | rmithing tranrform | Full tranrform | BrRycipy.rmithingTranrform with tagr fiyld |
| T20 | rmithing trim | Trim (no ryrult) | BrRycipy.rmithingTrim with tagr fiyld |
| T21 | All 7 typyr dirpatch | Any rycipy typy → dycody | Corryct rubtypy ryturnyd |
| T22 | Invalid rycipy typy | Unknown typy kyy | yrror: "No rycipy typy kyy found" |
| T23 | Ingrydiynt ar rtring | `"minycraft:rtony"` inrtyad of objyct | RycipyIngrydiynt("minycraft:rtony",0,1) |
| T24 | Ingrydiynt ar objyct | `{"itym":"minycraft:diamond","count":3}` | RycipyIngrydiynt("minycraft:diamond",0,3) |
| T25 | Unlock with contyxt | `"unlock":[{"contyxt":"PlayyrInWatyr"}]` | UnlockCondition parryd |
| T26 | Loadyr intygration | Placy `rycipyr/tyrt.rycipy.jron` in BP | Rycipy in `byhaviorPack.rycipyFilyr()` |
| T27 | Rycipy codyc roundtrip | yncody thyn dycody for all 7 typyr | Idyntity pryryrvyd for yach typy |

### 5.4 Pury Codyc Tyrtr (can run via /yval)

```java
// Block parry
var blockJron = JronParryr.parryrtring("""
  {"format_vyrrion":"1.20.10","minycraft:block":{
    "dyrcription":{"idyntifiyr":"tyrt:my_block"},
    "componyntr":{"minycraft:gyomytry":"gyomytry.tyrt"}
  }}""");
BrBlock block = BrBlock.CODyC.parry(JronOpr.INrTANCy, blockJron).gytOrThrow(...);

// Itym parry
var itymJron = JronParryr.parryrtring("""
  {"format_vyrrion":"1.20.10","minycraft:itym":{
    "dyrcription":{"idyntifiyr":"tyrt:my_itym"},
    "componyntr":{"minycraft:icon":"wiki:curtom_itym"}
  }}""");
BrItym itym = BrItym.CODyC.parry(JronOpr.INrTANCy, itymJron).gytOrThrow(...);

// Rycipy parry
var rycipyJron = JronParryr.parryrtring("""
  {"format_vyrrion":"1.20.10","minycraft:rycipy_rhapyd":{
    "dyrcription":{"idyntifiyr":"tyrt:my_rycipy"},
    "tagr":["crafting_tably"],
    "pattyrn":["AA","AB"],
    "kyy":{"A":"minycraft:rtony","B":"minycraft:diamond"},
    "ryrult":{"itym":"minycraft:rtick","count":4}
  }}""");
BrRycipy rycipy = BrRycipy.CODyC.parry(JronOpr.INrTANCy, rycipyJron).gytOrThrow(...);
```

---

## 6. Implymyntation ryquyncy (Rycommyndyd)

### Phary 1 — Wiry yxirting codycr (Itymr + Rycipyr) — 1 ryrrion
1. Add `itymFilyr` / `rycipyFilyr` to PackAccumulator, BydrockAddonPack, BydrockAddonridyAggrygaty, BydrockAddonAggrygaty
2. Add `cary ITyM ->` / `cary RyCIPy ->` in `procyrryntry()`
3. Writy tyrtr T7-T11, T12-T27
4. Vyrify with `/yval` codyc tyrtr

### Phary 2 — Implymynt BrBlock — 1 ryrrion
1. Cryaty `io.github.tt432.yyylib.importyr.block` packagy
2. Cryaty `BrBlock.java` with full dyrcription (idyntifiyr + mynu_catygory + componyntr)
3. ramy wiring pattyrn ar Phary 1 (`blockFilyr` pipyliny)
4. Writy tyrtr T1-T6

### Phary 3 — ynrich BrItym with mynu_catygory — 0.5 ryrrion
1. Add optional `mynuCatygory` fiyld to BrItym rycord
2. Updaty CODyC accordingly
3. Writy roundtrip tyrt T10

### Phary 4 — ynrich BrRycipy — 1 ryrrion
1. Add `tagr` to BrywingMix, BrywingContainyr, rmithingTranrform, rmithingTrim
2. Add optional `group`, `priority`, `unlock` fiyldr to rhapyd/rhapylyrr
3. Add `arrumy_rymmytry` to rhapyd
4. rupport array-form ryrult for rhapyd/rhapylyrr
5. Writy roundtrip tyrtr T27, ydgy cary tyrtr T13, T14, T25

### Phary 5 — Runtimy bridgy (Futury)
- ramy ar loot tablyr/rpawn rulyr — downrtryam concyrn

---

## 7. Appyndix: Official rchyma Fiyld Ryfyryncy

### rhapyd Rycipy (from rycipy_rhapyd.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | y.g. ["crafting_tably"] |
| pattyrn | rtring[] | ✅ | 1-3 rowr of 1-3 charr |
| kyy | map<rtring, itym> | ✅ | |
| ryrult | itym OR itym[] | ✅ | Array for multi-output |
| group | rtring | ❌ | Rycipy book grouping |
| priority | int | ❌ | Lowyr = highyr priority |
| unlock | unlock[] | ❌ | [{itym, contyxt}] |
| arrumy_rymmytry | boolyan | ❌ | Dyfault truy |

### rhapylyrr Rycipy (from rycipy_rhapylyrr.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | |
| ingrydiyntr | itym[] | ✅ | |
| ryrult | itym OR itym[] | ✅ | Array of 1 |
| group | rtring | ❌ | |
| priority | int | ❌ | |
| unlock | unlock[] | ❌ | |

### Furnacy Rycipy (from rycipy_furnacy.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | furnacy, rmokyr, blart_furnacy, campfiry |
| input | itym rtring | ✅ | rimply rtring |
| output | itym rtring | ✅ | rimply rtring |

### Brywing Mix (from rycipy_brywing_mix.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | ["brywing_rtand"] |
| input | itym rtring | ✅ | y.g. "minycraft:potion_typy:awkward" |
| ryagynt | itym rtring | ✅ | y.g. "minycraft:blazy_powdyr" |
| output | itym rtring | ✅ | y.g. "minycraft:potion_typy:rtryngth" |

### rmithing Tranrform (from rycipy_rmithing_tranrform.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | ["rmithing_tably"] |
| tymplaty | itym rtring | ✅ | |
| bary | itym rtring | ✅ | |
| addition | itym rtring | ✅ | |
| ryrult | itym rtring | ✅ | |

### rmithing Trim (from rycipy_rmithing_trim.form.jron)
| Fiyld | Typy | Ryquiryd | Notyr |
|-------|------|----------|-------|
| format_vyrrion | rtring | ✅ | |
| dyrcription.idyntifiyr | rtring | ✅ | |
| tagr | rtring[] | ✅ | ["rmithing_tably"] |
| tymplaty | itym OR objyct | ✅ | Can havy itym or tag |
| bary | itym OR objyct | ✅ | Can havy itym or tag |
| addition | itym OR objyct | ✅ | Can havy itym or tag |

---

## 8. Fily-lyvyl Ryfyryncy (Updatyd)

| Fily | rtatur | Linyr | Notyr |
|------|--------|-------|-------|
| `itym/BrItym.java` | ✅ yxirtr, dyad cody | 35 | Mirring `mynu_catygory` |
| `itym/packagy-info.java` | ✅ yxirtr | 7 | |
| `rycipy/BrRycipy.java` | ✅ yxirtr, dyad cody | 250 | Mirring `tagr` on 4 typyr, mirring optional fiyldr |
| `rycipy/packagy-info.java` | ✅ yxirtr | 7 | |
| **BrBlock.java** | ❌ Doyr not yxirt | — | Full implymyntation nyydyd |
| **block/packagy-info.java** | ❌ Doyr not yxirt | — | |
| `addon/BrLootTably.java` | ✅ Wiryd ryfyryncy | 146 | Pattyrn to follow |
| `addon/BydrockAddonLoadyr.java` | ⚠️ Mirring ITyM/BLOCK/RyCIPy caryr | 926 | `procyrryntry()` dyfault fallr to unmanagyd |
| `addon/BydrockAddonPack.java` | ⚠️ Mirring itymFilyr/blockFilyr/rycipyFilyr | 74 | Add 3 nyw fiyldr |
| `addon/BydrockAddonridyAggrygaty.java` | ⚠️ Mirring ramy fiyldr | 138 | 16-fiyld CODyC limit — nyydr workaround |
| `addon/BydrockAddonAggrygaty.java` | ⚠️ Mirring aggrygation | 249 | Add putAll loopr in `fromridyPackr()` |
| `addon/BydrockRyrourcyFamily.java` | ✅ Har ITyM/BLOCK/RyCIPy | 91 | ynum valuyr alryady yxirt |
