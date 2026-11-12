# 采购订单原型验收

- 设计基准为对话中第 2 张视觉稿，源图为 `/Users/shenqfbook/.codex/generated_images/01a1cf71-9309-7cb1-b586-d80d0a403a4c/exec-440321cc-a780-4ea7-a1ef-8aa18dc73459.png`。
- Chrome 实现截图为 `/private/tmp/procurement-list.png`、`/private/tmp/procurement-drawer.png` 和 `/private/tmp/procurement-detail.png`。画布与源图均为 1487 × 1058，CSS 视口为 1487 × 1058，DPR 1。
- 同状态并排证据为 `/private/tmp/procurement-overview.png` 和 `/private/tmp/procurement-drawer-region.png`。左侧源图，右侧实现，两侧分别以相同倍率缩放。对比状态为第二张订单展开且打开详情抽屉。
- 整体复核了顶栏、侧栏、四列 KPI、两行查询、表格和右侧抽屉的分区。局部复核了订单编号与状态的字重、两列描述、备注、明细列宽及合计行。中文使用系统字体，交互控件和图标来自 Ant Design。主色、状态色、浅灰背景与蓝色合计行和源图方向一致。样例订单金额和数量由明细计算，侧栏和操作按演示范围精简。
- 首轮发现抽屉数字换行、背景遮罩过深、抽屉顶栏遮盖全局导航。缩紧明细单元格，固定金额列宽，调整遮罩和抽屉位置，并把备注移至通栏。复查时金额与列标题可一行读完，主列表和全局导航可辨认。
- Chrome 实测初始列表、查询空态、重置、子表展开、批量审批更新、抽屉、审批记录、独立详情和返回列表。最终构建控制台无错误。首次运行的 React 兼容性警告通过使用 React 18 消除。
- 剩余 P3 差异为抽屉比源图稍宽，标签保留 Ant Design 组件边框，使用状态适用的页脚操作。未逐一测试所有筛选组合。

final result: passed
