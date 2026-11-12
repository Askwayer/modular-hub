import React, { useEffect, useState } from 'react';
import { App as AntApp, Button, Card, ConfigProvider, DatePicker, Descriptions, Drawer, Dropdown, Form, Input, InputNumber, Modal, Progress, Select, Space, Statistic, Table, Tabs, Tag, Timeline, Typography } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { ApartmentOutlined, ArrowLeftOutlined, BellOutlined, ClockCircleOutlined, DownloadOutlined, ExportOutlined, FileTextOutlined, HomeOutlined, PlusOutlined, ShopOutlined, TruckOutlined, WalletOutlined } from '@ant-design/icons';
import { amount, buyers, initialOrders, money, quantities, statuses, tags, warehouses } from './orders';

const options = values => values.map(value => ({ value, label:value }));
const statusTag = status => <Tag color={statuses[status]}>{status}</Tag>;
const tagList = values => values.length ? values.map(value => <Tag key={value} color={tags[value]}>{value}</Tag>) : <span className="muted">—</span>;

const lineColumns = [
  { title:'物料', dataIndex:'name' }, { title:'SKU', dataIndex:'sku' }, { title:'规格', dataIndex:'spec' },
  { title:'数量', dataIndex:'qty', align:'right', width:46 }, { title:'已收', dataIndex:'received', align:'right', width:46 },
  { title:'单价', dataIndex:'price', align:'right', width:88, className:'numeric', render:money },
  { title:'小计', align:'right', width:96, className:'numeric', render:(_, line) => money(line.qty * line.price) },
];
function Lines({ order, inline }) {
  const [qty, received] = quantities(order);
  return <Table className={inline ? 'line-items nested' : 'line-items'} size="small" rowKey="sku" columns={lineColumns} dataSource={order.lines} pagination={false} scroll={{ x:500 }}
    locale={{emptyText:'暂无物料明细'}} footer={order.lines.length ? () => <div className="line-total">合计 <span>数量 {qty.toLocaleString()}</span><span>已收 {received.toLocaleString()}</span><strong>{money(amount(order))}</strong></div> : undefined} />;
}
function Detail({ order }) {
  const [qty, received] = quantities(order);
  const receivedAmount = order.lines.reduce((total,line) => total+line.received*line.price,0);
  return <>
    <div className="order-title"><h2>{order.id}</h2>{statusTag(order.status)}</div>
    <Descriptions column={2} size="small" items={[
      {label:'供应商',children:order.vendor}, {label:'采购金额',children:<strong>{money(amount(order))}</strong>},
      {label:'采购员',children:order.buyer}, {label:'已收金额',children:money(receivedAmount)},
      {label:'订单日期',children:order.date}, {label:'未收金额',children:money(amount(order)-receivedAmount)},
      {label:'交货日期',children:order.due}, {label:'收货进度',children:`${received} / ${qty}（${qty ? Math.round(received/qty*100) : 0}%）`},
      {label:'仓库',children:order.warehouse}, {label:'标签',children:tagList(order.tags)},
      {label:'备注',span:2,className:'order-note',children:order.note || '—'},
    ]}/>
    <Tabs items={[
      {key:'lines',label:`订单明细（${order.lines.length}）`,children:<Lines order={order}/>},
      {key:'history',label:'审批记录',children:<Timeline className="history" items={order.history.slice().reverse().map(event => ({children:<><strong>{event.text}</strong><div className="muted">{event.date} · {event.actor}</div></>}))}/>},
    ]}/>
  </>;
}
function Prototype() {
  const [orders,setOrders] = useState(initialOrders);
  const [filters,setFilters] = useState({});
  const [selected,setSelected] = useState([]);
  const [expanded,setExpanded] = useState([]);
  const [drawer,setDrawer] = useState(null);
  const [route,setRoute] = useState(location.hash);
  const [page,setPage] = useState(1);
  const [pageSize,setPageSize] = useState(10);
  const [editing,setEditing] = useState(null);
  const [form] = Form.useForm();
  const [editor] = Form.useForm();
  const { message, modal } = AntApp.useApp();
  useEffect(() => {
    const readRoute = () => setRoute(location.hash);
    window.addEventListener('hashchange',readRoute);
    return () => window.removeEventListener('hashchange',readRoute);
  },[]);
  const detailId = route.startsWith('#/orders/') ? route.slice(9) : null;
  const active = orders.find(order => order.id === (detailId || drawer));
  const filtered = orders.filter(order => {
    const keyword = filters.keyword?.trim().toLowerCase();
    if (keyword && ![order.id,order.vendor,order.note].some(value => String(value ?? '').toLowerCase().includes(keyword))) return false;
    if (filters.buyer && order.buyer !== filters.buyer) return false;
    if (filters.status && order.status !== filters.status) return false;
    if (filters.warehouses?.length && !filters.warehouses.includes(order.warehouse)) return false;
    if (filters.tags?.length && !filters.tags.every(tag => order.tags.includes(tag))) return false;
    if (filters.date && (order.date < filters.date[0].format('YYYY-MM-DD') || order.date > filters.date[1].format('YYYY-MM-DD'))) return false;
    return (filters.min == null || amount(order) >= filters.min) && (filters.max == null || amount(order) <= filters.max);
  });
  const chosen = orders.filter(order => selected.includes(order.id));
  const sum = rows => rows.reduce((total,order) => total + amount(order),0);
  const apply = values => {
    if (values.min != null && values.max != null && values.min > values.max) {
      message.warning('最低金额须小于或等于最高金额');
      return;
    }
    setFilters(values); setPage(1); setSelected([]); setExpanded([]);
  };
  const openPage = order => { setDrawer(null); location.hash = '/orders/' + order.id; };
  const back = () => { location.hash = '/orders'; setDrawer(null); };
  const stamp = (text, actor = '张伟') => ({text, actor, date:new Intl.DateTimeFormat('sv-SE', {dateStyle:'short',timeStyle:'short',timeZone:'Asia/Shanghai'}).format(new Date())});
  const transition = (rows, status, text, fill = false) => {
    const ids = new Set(rows.map(order => order.id));
    setOrders(previous => previous.map(order => ids.has(order.id) ? {
      ...order, status, lines:fill ? order.lines.map(line => ({...line,received:line.qty})) : order.lines,
      history:[...order.history,stamp(text)],
    } : order));
    message.success(`已处理 ${rows.length} 张订单 · ${text}`);
  };
  const approve = rows => {
    const eligible = rows.filter(order => order.status === '待审批');
    if (!eligible.length) return message.warning('请选择待审批的订单');
    modal.confirm({title:`通过 ${eligible.length} 张订单的审批？`,content:eligible.length < rows.length ? `另外 ${rows.length-eligible.length} 张订单当前状态无需审批。` : '审批通过后，订单将进入待收货。',okText:'确认审批',onOk:() => {transition(eligible,'待收货','审批通过');setSelected([]);}});
  };
  const exportOrders = rows => {
    const csv = [['订单号','供应商','状态','金额（CNY）','仓库','订单日期'],...rows.map(order => [order.id,order.vendor,order.status,amount(order),order.warehouse,order.date])]
      .map(row => row.map(value => '"' + String(value).replaceAll('"','""') + '"').join(',')).join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF',csv],{type:'text/csv;charset=utf-8'}));
    const link = document.createElement('a');
    link.href=url; link.download='采购订单.csv'; link.click();
    setTimeout(() => URL.revokeObjectURL(url),1000);
    message.success(`已导出 ${rows.length} 张订单`);
  };
  const openEditor = order => {
    editor.resetFields();
    editor.setFieldsValue(order || {buyer:buyers[0],warehouse:warehouses[0],tags:[]});
    setEditing(order || {});
  };
  const save = values => {
    if (editing.id) {
      setOrders(previous => previous.map(order => order.id === editing.id ? {...order,...values,history:[...order.history,stamp('更新订单资料')]} : order));
    } else {
      const event = stamp('创建采购订单',values.buyer);
      const date = event.date.slice(0,10);
      const id = 'PO' + date.replaceAll('-','') + String(Date.now()).slice(-7);
      setOrders(previous => [{...values,id,date,due:'—',status:'草稿',note:values.note || '',tags:values.tags || [],lines:[],history:[event]},...previous]);
      form.resetFields(); apply({}); setDrawer(id);
    }
    setEditing(null); message.success('订单已保存');
  };
  const receive = order => modal.confirm({title:'确认全部物料已收货？',onOk:() => transition([order],'已完成','收货完成',true)});
  const actions = order => [
    ...(order.status === '待审批' ? [{key:'approve',label:'审批通过',onClick:() => approve([order])},{key:'reject',label:'驳回',onClick:() => transition([order],'已驳回','审批驳回')}] : []),
    ...(['待收货','部分收货'].includes(order.status) ? [{key:'receive',label:'确认全部收货',onClick:() => receive(order)}] : []),
    ...(['草稿','已驳回'].includes(order.status) ? [{key:'edit',label:'编辑订单',onClick:() => openEditor(order)}] : []),
    {key:'detail',label:'打开独立详情',onClick:() => openPage(order)},
    {key:'export',label:'导出订单',onClick:() => exportOrders([order])},
    ...(['草稿','待审批','已驳回','待收货'].includes(order.status) ? [{key:'cancel',label:'取消订单',danger:true,onClick:() => modal.confirm({title:'确认取消这张订单？',onOk:() => transition([order],'已取消','已取消')})}] : []),
  ];
  const columns = [
    {title:'订单号',dataIndex:'id',width:148,render:(id) => <Button type="link" className="table-link" onClick={() => setDrawer(id)}>{id}</Button>},
    {title:'供应商',dataIndex:'vendor',width:176,ellipsis:true},
    {title:'订单状态',dataIndex:'status',width:90,render:statusTag},
    {title:'标签',dataIndex:'tags',width:120,render:tagList},
    {title:'采购金额（CNY）',width:144,align:'right',sorter:(a,b) => amount(a)-amount(b),render:(_,order) => money(amount(order))},
    {title:'收货进度',width:116,render:(_,order) => {
      const [qty,received]=quantities(order);
      return qty ? <div className="progress"><span>{received} / {qty}</span><Progress percent={Math.round(received/qty*100)} showInfo={false} size="small"/></div> : '—';
    }},
    {title:'订单日期',dataIndex:'date',width:110,sorter:(a,b) => a.date.localeCompare(b.date)},
    {title:'采购员',dataIndex:'buyer',width:70},
    {title:'操作',width:110,fixed:'right',render:(_,order) => <Space size={9}><Button className="table-link" type="link" onClick={() => setDrawer(order.id)}>查看</Button><Dropdown menu={{items:actions(order)}} trigger={['click']}><Button className="table-link" type="link">更多⌄</Button></Dropdown></Space>},
  ];
  const detailActions = order => <Space>
    {order.status === '待审批' && <Button type="primary" onClick={() => approve([order])}>审批通过</Button>}
    {['待收货','部分收货'].includes(order.status) && <Button type="primary" onClick={() => receive(order)}>确认收货</Button>}
    <Dropdown menu={{items:actions(order).filter(item => !['detail','approve','receive'].includes(item.key))}} trigger={['click']}><Button>订单操作</Button></Dropdown>
  </Space>;
  return <>
    <header className="topbar"><div className="brand"><ApartmentOutlined/> 企业采购管理系统</div><span className="top-section">采购运营</span><div className="profile"><BellOutlined/><span className="avatar">张</span>张伟</div></header>
    <div className="layout">
      <aside className="sidenav"><div className="nav-caption">工作空间</div><div className="nav-item"><HomeOutlined/> 工作台</div><div className="nav-item nav-group"><ShopOutlined/> 采购管理</div><button className="nav-item nav-current" onClick={back}>采购订单</button><div className="nav-item nav-child">收货管理</div><div className="nav-item nav-child">供应商管理</div></aside>
      <main className="main">
        {detailId ? <>
          <div className="crumb">采购管理 / <button onClick={back}>采购订单</button> / 订单详情</div>
          <div className="page-head"><div><Button type="text" icon={<ArrowLeftOutlined/>} onClick={back}>返回列表</Button><h1>订单详情</h1></div>{active && detailActions(active)}</div>
          <Card className="detail-page">{active ? <Detail key={active.id} order={active}/> : <Typography.Text>未找到该订单</Typography.Text>}</Card>
        </> : <>
          <h1>采购订单</h1>
          <section className="kpis" aria-label="采购概览">
            {[
              ['订单总数',filtered.length,<FileTextOutlined/>,'blue','单'],
              ['待审批',filtered.filter(order => order.status==='待审批').length,<ClockCircleOutlined/>,'orange','单'],
              ['待收货',filtered.filter(order => ['待收货','部分收货'].includes(order.status)).length,<TruckOutlined/>,'blue','单'],
              ['采购金额',money(sum(filtered)),<WalletOutlined/>,'green'],
            ].map(([title,value,icon,color,suffix]) => <Card key={title} size="small"><div className="stat"><span className={'stat-icon '+color}>{icon}</span><Statistic title={title} value={value} suffix={suffix}/></div></Card>)}
          </section>
          <section className="surface filter-panel">
            <Form form={form} onFinish={apply} layout="vertical" className="filters" autoComplete="off">
              <Form.Item className="keyword" name="keyword" label="关键词"><Input allowClear placeholder="订单号 / 供应商 / 备注"/></Form.Item>
              <Form.Item name="buyer" label="采购员"><Select allowClear showSearch options={options(buyers)} placeholder="请选择采购员"/></Form.Item>
              <Form.Item name="date" label="订单日期"><DatePicker.RangePicker style={{width:'100%'}}/></Form.Item>
              <Form.Item label="采购金额"><Space.Compact block><Form.Item name="min" noStyle><InputNumber min={0} placeholder="最低金额" aria-label="最低金额" style={{width:'50%'}}/></Form.Item><Form.Item name="max" noStyle><InputNumber min={0} placeholder="最高金额" aria-label="最高金额" style={{width:'50%'}}/></Form.Item></Space.Compact></Form.Item>
              <Form.Item name="status" label="订单状态"><Select allowClear options={options(Object.keys(statuses))} placeholder="全部状态"/></Form.Item>
              <Form.Item name="warehouses" label="仓库"><Select mode="multiple" allowClear maxTagCount="responsive" options={options(warehouses)} placeholder="选择仓库"/></Form.Item>
              <Form.Item name="tags" label="标签"><Select mode="multiple" allowClear maxTagCount="responsive" options={options(Object.keys(tags))} placeholder="选择标签"/></Form.Item>
              <div className="query-actions"><Button type="primary" htmlType="submit">查询</Button><Button onClick={() => {form.resetFields();apply({});}}>重置</Button></div>
            </Form>
          </section>
          <section className="surface table-panel">
            <div className="toolbar"><Space wrap><Button type="primary" icon={<PlusOutlined/>} onClick={() => openEditor()}>新建订单</Button><Button disabled={!chosen.length} onClick={() => approve(chosen)}>批量审批</Button><Button disabled={!chosen.length} icon={<DownloadOutlined/>} onClick={() => exportOrders(chosen)}>批量导出</Button><span className="selection">已选 <b>{chosen.length}</b> 项</span><span className="muted">选中金额 <b>{money(sum(chosen))}</b></span>{chosen.length > 0 && <Button type="link" onClick={() => setSelected([])}>清空</Button>}</Space><span className="muted">共 {filtered.length} 条</span></div>
            <Table rowKey="id" size="small" dataSource={filtered} columns={columns} scroll={{x:1160}}
              rowSelection={{selectedRowKeys:selected,onChange:setSelected,preserveSelectedRowKeys:true,columnWidth:38}}
              expandable={{expandedRowKeys:expanded,onExpandedRowsChange:setExpanded,expandedRowRender:order => <Lines order={order} inline/>,columnWidth:34}}
              pagination={{current:page,pageSize,total:filtered.length,showSizeChanger:true,pageSizeOptions:[10,20,50],showTotal:total => `共 ${total} 条`,onChange:(p,size) => {setPage(p);setPageSize(size);}}}
              locale={{emptyText:'没有符合条件的订单'}}/>
          </section>
        </>}
      </main>
    </div>
    <Drawer open={!!drawer && !detailId && !!active} onClose={() => setDrawer(null)} width="min(530px, 100vw)" title="订单详情" rootStyle={{top:52}} styles={{mask:{backgroundColor:'rgba(25,35,50,.08)'},body:{padding:'18px 16px'}}}
      extra={active && <Button icon={<ExportOutlined/>} onClick={() => openPage(active)}>打开独立详情</Button>}
      footer={active && <div className="drawer-footer">{detailActions(active)}</div>}>
      {active && <Detail key={active.id} order={active}/>}
    </Drawer>
    <Modal title={editing?.id ? '编辑订单' : '新建采购订单'} open={!!editing} onCancel={() => setEditing(null)} onOk={() => editor.submit()} okText="保存">
      <Form layout="vertical" form={editor} onFinish={save}>
        <Form.Item label="供应商" name="vendor" rules={[{required:true,whitespace:true,message:'请输入供应商'}]}><Input/></Form.Item>
        <Form.Item label="采购员" name="buyer" rules={[{required:true}]}><Select options={options(buyers)}/></Form.Item>
        <Form.Item label="仓库" name="warehouse" rules={[{required:true}]}><Select options={options(warehouses)}/></Form.Item>
        <Form.Item label="标签" name="tags"><Select mode="multiple" options={options(Object.keys(tags))}/></Form.Item>
        <Form.Item label="备注" name="note"><Input.TextArea rows={2}/></Form.Item>
      </Form>
    </Modal>
  </>;
}
export function App() {
  return <ConfigProvider locale={zhCN} theme={{token:{colorPrimary:'#1677ff',borderRadius:6,fontFamily:'Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif'},components:{Table:{headerBg:'#f6f7f9',headerColor:'#252b37',cellPaddingBlockSM:6},Statistic:{contentFontSize:23}}}}><AntApp><Prototype/></AntApp></ConfigProvider>;
}
