---
type: frontend-architecture
status: active
updated: 2026-08-13
tags:
  - erp-ui
  - aceternity-ui
  - ux-optimization
---

# Aceternity UI 交互组件规范与集成指南

ERP 前端（`erp-ui`）基于 [Aceternity UI Labs](https://ui.aceternity.com/labs) 设计规范，封装了全套高科技感与平滑微交互的动效组件，提升企业级 UI/UX 体验。

## 核心交互组件目录 (`src/components/aceternity/`)

1. **`SpotlightCard.vue`**：光标跟随 3D 聚光灯卡片，`radial-gradient` 实时计算 `(x, y)` 坐标。应用于 Bento Grid 看板与统计数据块。
2. **`CommandPalette.vue`**：全局 `Ctrl+K` 命令检索面板，支持菜单模糊匹配、键盘上下键导航与直达。
3. **`FloatingDock.vue`**：底部/右下角磁性悬浮工具栏，基于距离衰减公式实现图标平滑放大与点按反馈。
4. **`ShimmerButton.vue`**：扫光/流光主操作按钮，自带 `shimmer-sweep` 光效遮罩。
5. **`VanishInput.vue`**：智能打字切换占位符搜索框，清空时触发 Canvas 粒子散开解构动画。
6. **`MultiStepLoader.vue`**：沉浸式多步骤进度加载器，用于复杂长耗时算法与批量导入处理。
7. **`InfiniteMovingCards.vue`**：无尽平滑轮播卡片流，支持鼠标悬浮暂停，展示实时系统预警与通知。

## 主题与色值守卫规则

- 所有组件 CSS 强制遵守 `src/styles/palette.css` 语义色板 token（如 `--erp-c-surface`, `--erp-c-fill`, `--erp-c-border`, `--erp-c-text-strong`）。
- 打包前会自动通过 `npm run lint:colors` 色值守卫检查，确保在浅色与 Executive 深色模式下无缝适配。
