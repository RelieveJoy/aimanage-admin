package com.aimanage.admin;

import com.aimanage.admin.dto.CreateDepartmentRequest;
import com.aimanage.admin.dto.DepartmentVO;
import com.aimanage.admin.dto.UpdateDepartmentRequest;
import com.aimanage.audit.AuditContext;
import com.aimanage.audit.Auditable;
import com.aimanage.common.BizException;
import com.aimanage.entity.AuditLog;
import com.aimanage.entity.Department;
import com.aimanage.entity.User;
import com.aimanage.mapper.DepartmentMapper;
import com.aimanage.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织架构业务逻辑（AD3）。
 *
 * <p>树是<b>两层</b>的：公司（根，parentId = 0）→ 部门（parentId = 公司 ID）。
 * 不允许在部门下再建子部门 —— 单企业课设场景下三层以上无真实收益。
 */
@Service
@RequiredArgsConstructor
public class AdminDepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    // ---------------------------------------------------------------- 查询

    /**
     * 返回完整部门树。根节点是公司本身。
     */
    public List<DepartmentVO> tree() {
        List<Department> all = departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().orderByAsc(Department::getSort));

        // 一次查出各部门人数，避免在循环里逐条 count
        Map<Long, Integer> countByDept = countMembersByDept();

        // 按父节点分组，再自顶向下拼装
        Map<Long, List<Department>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));

        List<DepartmentVO> roots = new ArrayList<>();
        for (Department d : byParent.getOrDefault(0L, List.of())) {
            roots.add(build(d, byParent, countByDept));
        }
        return roots;
    }

    private DepartmentVO build(Department d,
                               Map<Long, List<Department>> byParent,
                               Map<Long, Integer> countByDept) {
        DepartmentVO vo = toVO(d, countByDept);
        List<Department> children = byParent.getOrDefault(d.getId(), List.of());
        children.stream()
                .sorted(Comparator.comparing(Department::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(c -> vo.getChildren().add(toVO(c, countByDept)));
        return vo;
    }

    private DepartmentVO toVO(Department d, Map<Long, Integer> countByDept) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setParentId(d.getParentId());
        vo.setSort(d.getSort());
        vo.setIsRoot(d.isRoot());
        vo.setMemberCount(countByDept.getOrDefault(d.getId(), 0));
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    /** 各部门的直属人数 */
    private Map<Long, Integer> countMembersByDept() {
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery().isNotNull(User::getDeptId));
        return users.stream()
                .collect(Collectors.groupingBy(
                        User::getDeptId,
                        Collectors.summingInt(u -> 1)));
    }

    /** 公司根节点，树为空时返回 null */
    public Department findRoot() {
        return departmentMapper.selectOne(
                Wrappers.<Department>lambdaQuery().eq(Department::getParentId, 0L).last("LIMIT 1"));
    }

    // ---------------------------------------------------------------- 新增

    @Auditable(type = AuditLog.TARGET_DEPARTMENT, action = AuditLog.ACTION_CREATE)
    @Transactional
    public DepartmentVO create(CreateDepartmentRequest req) {
        Department parent = resolveParent(req.getParentId());

        Department d = new Department();
        d.setName(req.getName());
        d.setParentId(parent.getId());
        d.setSort(req.getSort() == null ? 0 : req.getSort());
        departmentMapper.insert(d);

        AuditContext.targetId(d.getId());
        AuditContext.targetName(d.getName());
        AuditContext.change("部门", null, d.getName());

        return toVO(d, Map.of());
    }

    /**
     * 校验父节点：必须存在，且必须是公司根节点（否则就变成三层了）。
     */
    private Department resolveParent(Long parentId) {
        Department parent;
        if (parentId == null) {
            parent = findRoot();
            if (parent == null) {
                throw BizException.conflict("公司根节点尚未初始化");
            }
        } else {
            parent = departmentMapper.selectById(parentId);
            if (parent == null) {
                throw BizException.notFound("上级部门不存在");
            }
        }
        if (!parent.isRoot()) {
            throw BizException.badRequest("组织架构只支持两层，不能在部门下再建子部门");
        }
        return parent;
    }

    // ---------------------------------------------------------------- 修改

    @Auditable(type = AuditLog.TARGET_DEPARTMENT, action = AuditLog.ACTION_UPDATE, targetIdArg = 0)
    @Transactional
    public DepartmentVO update(Long id, UpdateDepartmentRequest req) {
        Department d = requireDepartment(id);
        AuditContext.targetName(d.getName());

        if (req.getName() != null && !req.getName().equals(d.getName())) {
            AuditContext.change("部门名称", d.getName(), req.getName());
            d.setName(req.getName());
        }
        if (req.getSort() != null && !req.getSort().equals(d.getSort())) {
            AuditContext.change("排序", d.getSort(), req.getSort());
            d.setSort(req.getSort());
        }
        departmentMapper.updateById(d);
        return toVO(d, countMembersByDept());
    }

    // ---------------------------------------------------------------- 删除

    @Auditable(type = AuditLog.TARGET_DEPARTMENT, action = AuditLog.ACTION_DELETE, targetIdArg = 0)
    @Transactional
    public void delete(Long id) {
        Department d = requireDepartment(id);

        if (d.isRoot()) {
            throw BizException.conflict("公司根节点不能删除");
        }

        Long children = departmentMapper.selectCount(
                Wrappers.<Department>lambdaQuery().eq(Department::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.conflict("该部门下仍有子部门，无法删除");
        }

        Long members = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getDeptId, id));
        if (members != null && members > 0) {
            throw BizException.conflict("该部门下仍有成员，无法删除");
        }

        departmentMapper.deleteById(id);

        AuditContext.targetName(d.getName());
        AuditContext.change("部门", d.getName(), null);
    }

    // ---------------------------------------------------------------- 内部

    private Department requireDepartment(Long id) {
        Department d = departmentMapper.selectById(id);
        if (d == null) {
            throw BizException.notFound("部门不存在");
        }
        return d;
    }
}
