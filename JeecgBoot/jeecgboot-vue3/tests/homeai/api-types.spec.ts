/**
 * homeai 类型冒烟测试（不导入 defHttp，避免拉起完整 axios 依赖链）
 */
import type {
  HomeaiBill,
  HomeaiCategory,
  HomeaiFamily,
  HomeaiLearnMaterial,
  HomeaiPageParams,
  HomeaiPlan,
  HomeaiRecipe,
  HomeaiRecipeIngredient,
  HomeaiStorageFile,
  HomeaiUser,
} from '../../src/api/homeai/types';

test('HomeaiPageParams 结构可用', () => {
  const params: HomeaiPageParams = { pageNo: 1, pageSize: 20 };
  expect(params.pageNo).toBe(1);
  expect(params.pageSize).toBe(20);
});

test('核心实体类型可赋最小字段', () => {
  const user: HomeaiUser = { id: 'u1' };
  const family: HomeaiFamily = { id: 'f1', name: '测试家庭' };
  const bill: HomeaiBill = { id: 'b1' };
  const plan: HomeaiPlan = { id: 'p1' };
  const recipe: HomeaiRecipe = { id: 'r1', name: '蛋炒饭' };
  const learn: HomeaiLearnMaterial = { id: 'l1', title: '资料' };
  const file: HomeaiStorageFile = { id: 's1' };
  const cat: HomeaiCategory = { id: 'c1', name: '分类' };

  expect(user.id).toBe('u1');
  expect(family.name).toBe('测试家庭');
  expect(bill.id).toBe('b1');
  expect(plan.id).toBe('p1');
  expect(recipe.name).toBe('蛋炒饭');
  expect(learn.title).toBe('资料');
  expect(file.id).toBe('s1');
  expect(cat.name).toBe('分类');
});

test('HomeaiRecipeIngredient 支持 quantity/unit', () => {
  const ing: HomeaiRecipeIngredient = { name: '鸡蛋', quantity: 2, unit: '个' };
  expect(ing.quantity).toBe(2);
  expect(ing.unit).toBe('个');
});
