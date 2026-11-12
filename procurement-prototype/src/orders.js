const vendors = ['苏州华科电子有限公司','深圳市联创科技有限公司','上海中恒机电有限公司','宁波海泰工业品有限公司','广州合力五金有限公司','深圳华科电子有限公司','北京信联电子有限公司','杭州科瑞电子有限公司'];
export const statuses = { 草稿:'default', 待审批:'orange', 已驳回:'red', 待收货:'cyan', 部分收货:'blue', 已完成:'green', 已取消:'default' };
export const warehouses = ['华东主仓','华南仓','深圳仓','华北仓'];
export const buyers = ['张伟','李娜','王强'];
export const tags = { 紧急:'red', 框架采购:'purple', 首单:'cyan', 补货:'gold' };
const sequence = ['待审批','部分收货','已完成','部分收货','已取消','已完成','待收货','待审批','已驳回','草稿'];
const products = [
  ['工业交换机','SW-IG-2408','8 口千兆 / 工业级',4,3800],
  ['电源适配器','PWR-24V-120','24V / 5A',10,260],
  ['六类网线','CAB-CAT6-305','Cat6 / 305m',6,720],
];
export const money = value => '¥ ' + value.toLocaleString('zh-CN', { minimumFractionDigits:2, maximumFractionDigits:2 });
export const amount = order => order.lines.reduce((total, line) => total + line.qty * line.price, 0);
export const quantities = order => order.lines.reduce(([qty, received], line) => [qty + line.qty, received + line.received], [0, 0]);
export const initialOrders = Array.from({length:36}, (_, i) => {
  const status = sequence[i % sequence.length];
  const date = '2026-11-' + String(12 - Math.floor(i / 5)).padStart(2,'0');
  return {
    id: 'PO' + date.replaceAll('-','') + String(37-i).padStart(4,'0'),
    vendor: vendors[i % vendors.length], buyer: buyers[i % buyers.length], warehouse: warehouses[i % warehouses.length],
    date, due:'2026-11-20', status, tags: [[],['框架采购'],['补货','首单'],['紧急']][(i+3)%4],
    note: i === 1 ? '按季度框架协议补充采购，分批交付。' : ['生产备料，按计划交付。','到货后请通知质检。',''][i % 3],
    lines: i === 9 ? [] : products.map(([name,sku,spec,base,price], j) => {
      const qty = base * (i === 1 ? 1 : (i % 6 + 2) * 3);
      return {name,sku,spec,qty,price,received:status === '已完成' ? qty : status === '部分收货' ? Math.floor(qty * [0.5,0.4,0.33][j]) : 0};
    }),
    history: [{text:'创建采购订单', date:date+' 09:30', actor:buyers[i % buyers.length]}, ...(status === '草稿' ? [] : [{text:'提交审批', date:date+' 10:15', actor:buyers[i % buyers.length]}]), ...(['待审批','草稿'].includes(status) ? [] : [{text:status === '已驳回' ? '审批驳回' : status === '已取消' ? '订单已取消' : '审批通过',date:date+' 11:00',actor:'采购主管'}])],
  };
});
