# Patches for X (Twitter) app.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=serajr/x-twitter-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.10.0-dev.1](https://github.com/serajr/x-twitter-patches/releases/tag/v1.10.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 X (Twitter)&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 🧪&nbsp;12.33.0-alpha.02 | 🧪&nbsp;12.31.0-prod.01 | 12.29.1-prod.01 |
| :---: | :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Corrige o contraste da Barra de Navegação](#corrige-o-contraste-da-barra-de-navega-o) | Corrige o bug de transparência total forçando o contraste nativo! |  |
| [Desativar Blur do X](#desativar-blur-do-x) | Desativa permanentemente os efeitos de desfoque da Haze no X 12.30+. |  |
| [Preservar contraste da Barra de Navegação](#preservar-contraste-da-barra-de-navega-o) | Mantém permanentemente a proteção visual da barra de navegação sem alterar o edge-to-edge. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

This repo is licensed under the [GNU General Public License v3.0](LICENSE)
