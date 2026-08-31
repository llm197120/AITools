import { formatQuantityUnit, parseAmountToQuantityUnit } from '../../src/views/homeai/utils/recipeIngredient';

test('拆出数字与单位', () => {
  expect(parseAmountToQuantityUnit('200克')).toEqual({ quantity: 200, unit: '克' });
  expect(parseAmountToQuantityUnit('1.5勺')).toEqual({ quantity: 1.5, unit: '勺' });
});

test('纯文字只当单位', () => {
  expect(parseAmountToQuantityUnit('适量')).toEqual({ unit: '适量' });
});

test('空串返回空对象', () => {
  expect(parseAmountToQuantityUnit('')).toEqual({});
  expect(parseAmountToQuantityUnit('   ')).toEqual({});
});

test('拼接数量与单位', () => {
  expect(formatQuantityUnit(200, '克')).toBe('200克');
  expect(formatQuantityUnit('1.5', '勺')).toBe('1.5勺');
});

test('无数量时用单位或兜底', () => {
  expect(formatQuantityUnit(undefined, '适量')).toBe('适量');
  expect(formatQuantityUnit(null, null, '少许')).toBe('少许');
});
