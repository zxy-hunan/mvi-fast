package com.mvi.ui.widget.brvlayout

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.mvi.core.base.UiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 多选列表组件
 * 基于 BaseListWidget 扩展的多选功能
 *
 * 特性：
 * 1. 支持单选/多选模式
 * 2. 自动维护选中状态
 * 3. 提供选中项变更回调
 * 4. 支持全选/反选/清空
 *
 * @param T 列表数据类型
 */
class MultiSelectListWidget<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseListWidget<T>(context, attrs, defStyleAttr) {

    // ========== 多选相关属性 ==========

    /**
     * 选中模式
     */
    enum class SelectionMode {
        NONE,       // 不可选
        SINGLE,     // 单选
        MULTIPLE    // 多选
    }

    // 当前选择模式
    private var selectionMode = SelectionMode.NONE

    // 选中的位置集合
    private val selectedPositions = mutableSetOf<Int>()

    // 选中的数据集合
    private val selectedItems = mutableListOf<T>()

    // 是否启用长按进入选择模式
    private var longPressToSelect = true

    // ========== 回调 ==========

    /**
     * 选中项变化回调
     * @param items 选中的数据列表
     * @param positions 选中的位置列表
     */
    var onSelectionChanged: ((items: List<T>, positions: List<Int>) -> Unit)? = null

    /**
     * 点击选中项回调（用于单选模式）
     * @param item 选中的数据
     * @param position 位置
     */
    var onItemClicked: ((item: T, position: Int) -> Unit)? = null

    /**
     * 长按进入选择模式回调
     */
    var onEnterSelectionMode: (() -> Unit)? = null

    /**
     * 退出选择模式回调
     */
    var onExitSelectionMode: (() -> Unit)? = null

    // ========== 配置方法 ==========

    /**
     * 设置选择模式
     * @param mode 选择模式
     */
    fun setSelectionMode(mode: SelectionMode) {
        this.selectionMode = mode
        if (mode == SelectionMode.NONE) {
            clearSelection()
        }
    }

    /**
     * 设置是否启用长按进入选择模式
     * @param enable 是否启用
     */
    fun setLongPressToSelect(enable: Boolean) {
        this.longPressToSelect = enable
    }

    // ========== 选择操作 ==========

    /**
     * 切换选中状态
     * @param position 位置
     * @return 是否选中
     */
    fun toggleSelection(position: Int): Boolean {
        val item = getRecyclerView().models?.getOrNull(position) as? T ?: return false

        return when (selectionMode) {
            SelectionMode.SINGLE -> {
                // 单选：清空其他，选中当前
                selectedPositions.clear()
                selectedItems.clear()
                selectedPositions.add(position)
                selectedItems.add(item)
                notifySelectionChanged()
                true
            }
            SelectionMode.MULTIPLE -> {
                // 多选：切换状态
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                    selectedItems.remove(item)
                } else {
                    selectedPositions.add(position)
                    selectedItems.add(item)
                }
                notifySelectionChanged()
                selectedPositions.contains(position)
            }
            SelectionMode.NONE -> false
        }
    }

    /**
     * 选中指定位置
     * @param position 位置
     * @return 是否成功
     */
    fun selectItem(position: Int): Boolean {
        if (selectionMode == SelectionMode.NONE) return false

        val item = getRecyclerView().models?.getOrNull(position) as? T ?: return false

        return when (selectionMode) {
            SelectionMode.SINGLE -> {
                selectedPositions.clear()
                selectedItems.clear()
                selectedPositions.add(position)
                selectedItems.add(item)
                notifySelectionChanged()
                true
            }
            SelectionMode.MULTIPLE -> {
                if (!selectedPositions.contains(position)) {
                    selectedPositions.add(position)
                    selectedItems.add(item)
                    notifySelectionChanged()
                }
                true
            }
            SelectionMode.NONE -> false
        }
    }

    /**
     * 取消选中指定位置
     * @param position 位置
     * @return 是否成功
     */
    fun deselectItem(position: Int): Boolean {
        if (selectionMode == SelectionMode.NONE) return false

        if (selectedPositions.contains(position)) {
            val item = getRecyclerView().models?.getOrNull(position) as? T
            selectedPositions.remove(position)
            item?.let { selectedItems.remove(it) }
            notifySelectionChanged()
            return true
        }
        return false
    }

    /**
     * 全选
     */
    fun selectAll() {
        if (selectionMode != SelectionMode.MULTIPLE) return

        val models = getRecyclerView().models as? List<T> ?: return

        selectedPositions.clear()
        selectedItems.clear()

        models.forEachIndexed { index, item ->
            selectedPositions.add(index)
            selectedItems.add(item)
        }

        notifySelectionChanged()
    }

    /**
     * 反选
     */
    fun invertSelection() {
        if (selectionMode != SelectionMode.MULTIPLE) return

        val models = getRecyclerView().models as? List<T> ?: return
        val newSelectedPositions = mutableSetOf<Int>()
        val newSelectedItems = mutableListOf<T>()

        models.forEachIndexed { index, item ->
            if (!selectedPositions.contains(index)) {
                newSelectedPositions.add(index)
                newSelectedItems.add(item)
            }
        }

        selectedPositions.clear()
        selectedItems.clear()
        selectedPositions.addAll(newSelectedPositions)
        selectedItems.addAll(newSelectedItems)

        notifySelectionChanged()
    }

    /**
     * 清空选择
     */
    fun clearSelection() {
        if (selectedPositions.isEmpty()) return

        selectedPositions.clear()
        selectedItems.clear()
        notifySelectionChanged()
    }

    /**
     * 退出选择模式
     */
    fun exitSelectionMode() {
        clearSelection()
        selectionMode = SelectionMode.NONE
        onExitSelectionMode?.invoke()
    }

    // ========== 查询方法 ==========

    /**
     * 获取选中的数据列表
     * @return 选中的数据列表
     */
    fun getSelectedItems(): List<T> = selectedItems.toList()

    /**
     * 获取选中的位置列表
     * @return 选中的位置列表
     */
    fun getSelectedPositions(): List<Int> = selectedPositions.toList()

    /**
     * 获取选中数量
     * @return 选中数量
     */
    fun getSelectedCount(): Int = selectedPositions.size

    /**
     * 判断指定位置是否选中
     * @param position 位置
     * @return 是否选中
     */
    fun isSelected(position: Int): Boolean = selectedPositions.contains(position)

    /**
     * 判断是否在选择模式
     * @return 是否在选择模式
     */
    fun isInSelectionMode(): Boolean = selectionMode != SelectionMode.NONE

    // ========== setup 扩展 ==========

    /**
     * 配置列表项（带多选支持）
     *
     * 使用示例：
     * ```kotlin
     * binding.multiSelectListWidget.setupWithSelection<UserModel>(
     *     itemResId = R.layout.item_user,
     *     onBind = { model, isSelected ->
     *         val binding = ItemUserBinding.bind(itemView)
     *         binding.tvName.text = model.name
     *         binding.checkBox.isChecked = isSelected
     *
     *         // 设置点击事件
     *         binding.root.setOnClickListener {
     *             // 处理点击，需要手动调用 toggleSelection
     *             val position = adapterPosition
     *             binding.multiSelectListWidget.toggleSelection(position)
     *             binding.multiSelectListWidget.refreshItem(position)
     *         }
     *     }
     * )
     * ```
     *
     * @param itemResId item 布局资源 ID
     * @param onBind 绑定回调（参数：model, isSelected）
     */
    inline fun <reified M : T> setupWithSelection(
        itemResId: Int,
        noinline onBind: (model: M, isSelected: Boolean) -> Unit
    ) {
        val rv = getRecyclerView()

        // 设置绑定和事件（addType 必须在 setup 块内调用）
        rv.setup {
            // 先添加类型映射
            addType<M>(itemResId)

            onBind {
                @Suppress("UNCHECKED_CAST")
                val model = getModel<M>() as M
                val position = adapterPosition
                val selected = this@MultiSelectListWidget.isSelected(position)

                // 调用用户的绑定回调
                onBind(model, selected)
            }
        }
    }

    // ========== 绑定 UiState ==========

    /**
     * 绑定 UiState（继承自 BaseListWidget，保持一致）
     */
    override fun bindData(
        lifecycleOwner: LifecycleOwner,
        uiStateFlow: StateFlow<UiState<List<T>>>,
        onSuccess: ((List<T>) -> Unit)?
    ) {
        super.bindData(lifecycleOwner, uiStateFlow, onSuccess)
    }

    // ========== 私有方法 ==========

    /**
     * 通知选中状态变化
     */
    private fun notifySelectionChanged() {
        onSelectionChanged?.invoke(selectedItems.toList(), selectedPositions.toList())

        // 如果没有选中项，自动退出选择模式
        if (selectedPositions.isEmpty() && selectionMode != SelectionMode.NONE) {
            // 可选：自动退出选择模式
            // exitSelectionMode()
        }
    }
}
