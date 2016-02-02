package cheetah.domain;

import java.util.List;
import java.util.Map;

/**
 * Created by Max on 2016/1/6.
 */
public class PageRequest extends AbstractPageable  implements Enquirer {
    private final Enquirer enquirer = new EnquirerImpl();

    public PageRequest(int page, int size) {
        super(page, size);
    }

    @Override
    public final OrderList getOrderList() {
        return enquirer.getOrderList();
    }

    @Override
    public boolean hasWhere() {
        return enquirer.hasWhere();
    }

    @Override
    public final void orderby(String property, Order.Direction order) {
        enquirer.orderby(property, order);
    }

    @Override
    public final void and(String name, Object value) {
        enquirer.and(name, value);
    }

    @Override
    public final void or(String name, Object value) {
        enquirer.or(name, value);
    }

    @Override
    public final void like(String name, String value) {
        enquirer.like(name, value);
    }

    @Override
    public void in(String property, List<Object> params) {
        enquirer.in(property, params);
    }

    @Override
    public void notIn(String property, List<Object> params) {
        enquirer.notIn(property, params);
    }

    @Override
    public void isNull(String property) {
        enquirer.isNull(property);
    }

    @Override
    public void notNull(String property) {
        enquirer.notNull(property);
    }

    @Override
    public void between(String property, Number start, Number end) {
        enquirer.between(property, start, end);
    }

    @Override
    public void eq(String property, Object value) {

    }

    @Override
    public void gt(String property, Number value) {
        enquirer.gt(property, value);
    }

    @Override
    public void lt(String property, Number value) {
        enquirer.lt(property, value);
    }

    @Override
    public void ge(String property, Number value) {
        enquirer.ge(property, value);
    }

    @Override
    public void le(String property, Number value) {
        enquirer.le(property, value);
    }

    @Override
    public Map<String, List<Object>> getIn() {
        return enquirer.getIn();
    }

    @Override
    public Map<String, List<Object>> getNotIn() {
        return enquirer.getNotIn();
    }

    @Override
    public String getIsNull() {
        return enquirer.getIsNull();
    }

    @Override
    public String getNotNull() {
        return enquirer.getNotNull();
    }

    @Override
    public Map<String, Number> getGt() {
        return enquirer.getGt();
    }

    @Override
    public Map<String, Number> getLt() {
        return enquirer.getLt();
    }

    @Override
    public Map<String, Number> getGe() {
        return enquirer.getGe();
    }

    @Override
    public Map<String, Number> getLe() {
        return enquirer.getLe();
    }

    @Override
    public void clearAll() {
        enquirer.clearAll();
    }

    public final Map<String, String> getLike() {
        return enquirer.getLike();
    }

    public final Map<String, Object> getOr() {
        return enquirer.getOr();
    }

    public final Map<String, Object> getAnd() {
        return enquirer.getAnd();
    }

    @Override
    public EnquirerImpl.Between getBetween() {
        return enquirer.getBetween();
    }

    @Override
    public int getNextPage() {
        return getPageNo() + 1;
    }

    @Override
    public int getPrePage() {
        return hasPrevious() ? getNextPage() - 1 : first();
    }

    @Override
    public int first() {
        return 0;
    }
}
