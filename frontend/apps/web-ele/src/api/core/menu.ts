import type { RouteRecordStringComponent } from '@vben/types';

import { requestClient } from '#/api/request';

interface BackendRoute {
  children?: BackendRoute[];
  component: string;
  hidden?: boolean;
  meta?: {
    icon?: string;
    isCache?: number;
    menuName: string;
  };
  path: string;
  routeLink?: string;
  routeName: string;
}

function mapBackendRoute(route: BackendRoute): RouteRecordStringComponent {
  return {
    children: route.children?.map(mapBackendRoute),
    component: route.component,
    meta: {
      hideInMenu: route.hidden,
      icon: route.meta?.icon,
      keepAlive: route.meta?.isCache === 1,
      title: route.meta?.menuName || route.routeName,
    },
    name: route.routeName,
    path: route.path || route.routeLink || '/',
  };
}

/**
 * 获取用户所有菜单
 */
export async function getAllMenusApi() {
  const routes = await requestClient.get<BackendRoute[]>('/auth/routers');
  return routes.map(mapBackendRoute);
}
