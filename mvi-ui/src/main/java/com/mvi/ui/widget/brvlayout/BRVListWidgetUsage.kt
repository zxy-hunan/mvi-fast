/**
 * BRV通用列表组件使用说明
 *
 * 核心特性：
 * 1. 只需 configure + setup 两步即可完成初始化
 * 2. 自动处理 UiState 状态变化（Loading、Empty、Error、Success）
 * 3. 自动处理分页逻辑（第一页/加载更多）
 * 4. 内置骨架屏和缺省页
 *
 * ========== 简化用法（推荐）==========
 *
 * 1. 在布局中使用 BaseListWidget：
 *    <com.mvi.ui.widget.brvlayout.BaseListWidget
 *        android:id="@+id/listWidget"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent" />
 *
 * 2. 在 Fragment 中使用（只需两步！）：
 *
 *    class UserListFragment : Fragment() {
 *        private val viewModel by viewModels<UserListViewModel>()
 *
 *        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *            super.onViewCreated(view, savedInstanceState)
 *
 *            // 第一步：configure 配置组件
 *            binding.listWidget.configure {
 *                enableRefresh = true
 *                enableLoadMore = true
 *                onRefresh = { viewModel.loadData(1) }
 *                onLoadMore = { page -> viewModel.loadData(page) }
 *            }
 *
 *            // 第二步：setup 配置列表项
 *            binding.listWidget.setup {
 *                addType<UserModel>(R.layout.item_user)
 *                onBind {
 *                    val model = getModel<UserModel>()
 *                    // 使用 ViewBinding
 *                    val itemBinding = ItemUserBinding.bind(itemView)
 *                    itemBinding.tvName.text = model.name
 *                    itemBinding.tvEmail.text = model.email
 *                }
 *                R.id.itemView.onClick {
 *                    val model = getModel<UserModel>()
 *                    toast("点击: ${model.name}")
 *                }
 *            }
 *
 *            // 第三步：bindData 自动处理状态（一行代码！）
 *            binding.listWidget.bindData(this, viewModel.uiState)
 *        }
 *    }
 *
 * ========== ViewModel 示例 ==========
 *
 *    class UserListViewModel : ViewModel() {
 *        private val _uiState = MutableStateFlow<UiState<List<UserModel>>>(UiState.Idle)
 *        val uiState: StateFlow<UiState<List<UserModel>>> = _uiState
 *
 *        fun loadData(page: Int) {
 *            viewModelScope.launch {
 *                _uiState.value = UiState.Loading()
 *                when (val result = repository.getUsers(page)) {
 *                    is ApiResult.Success -> {
 *                        if (result.data.isEmpty()) {
 *                            _uiState.value = UiState.Empty()
 *                        } else {
 *                            _uiState.value = UiState.Success(result.data)
 *                        }
 *                    }
 *                    is ApiResult.Error -> {
 *                        _uiState.value = UiState.Error(result.message)
 *                    }
 *                }
 *            }
 *        }
 *    }
 *
 * ========== 高级用法：带分页信息 ==========
 *
 * 如果后端返回包含分页信息的响应，可以使用 setPageInfoExtractor：
 *
 *    // 定义包含分页信息的响应
 *    data class PageResponse<T>(
 *        val data: List<T>,
 *        val page: Int,
 *        val pageSize: Int,
 *        val total: Int
 *    )
 *
 *    // ViewModel 返回 UiState<PageResponse<UserModel>>
 *    private val _uiState = MutableStateFlow<UiState<PageResponse<UserModel>>>(UiState.Idle)
 *    val uiState: StateFlow<UiState<PageResponse<UserModel>>> = _uiState
 *
 *    // Fragment 中使用
 *    binding.listWidget.configure {
 *        enableRefresh = true
 *        enableLoadMore = true
 *        onRefresh = { viewModel.loadData(1) }
 *        onLoadMore = { page -> viewModel.loadData(page) }
 *    }
 *
 *    binding.listWidget.setup {
 *        addType<UserModel>(R.layout.item_user)
 *        onBind {
 *            val model = getModel<UserModel>()
 *            // 使用 ViewBinding
 *            val itemBinding = ItemUserBinding.bind(itemView)
 *            itemBinding.tvName.text = model.name
 *        }
 *    }
 *
 *    // 设置分页信息提取器
 *    binding.listWidget.setPageInfoExtractor { item ->
 *        // item 是列表中的任意一项（所有项都有相同的分页信息）
 *        // 这里假设每个 UserModel 包含分页信息
 *        // 或者你可以用其他方式获取分页信息
 *        PageInfo(
 *            page = viewModel.currentPage,
 *            pageSize = 20,
 *            total = viewModel.totalCount
 *        )
 *    }
 *
 *    // 绑定状态（注意：这里泛型是 PageResponse<UserModel>）
 *    // 需要额外处理提取 data 部分
 *    lifecycleScope.launch {
 *        viewModel.uiState.collect { state ->
 *            when (state) {
 *                is UiState.Loading -> binding.listWidget.showLoading()
 *                is UiState.Empty -> binding.listWidget.showEmpty()
 *                is UiState.Error -> binding.listWidget.showError(state.message)
 *                is UiState.Success -> {
 *                    // 提取 data 并设置
 *                    binding.listWidget.setModels(state.response.data)
 *                }
 *                else -> {}
 *            }
 *        }
 *    }
 *
 * ========== 配置选项 ==========
 *
 *    binding.listWidget.configure {
 *        enableRefresh = true              // 启用下拉刷新
 *        enableLoadMore = true             // 启用加载更多
 *        emptyMessage = "暂无数据"         // 空数据提示
 *        errorMessage = "加载失败"         // 错误提示
 *        skeletonLayoutResId = R.layout.item_skeleton_custom  // 自定义骨架屏布局
 *        skeletonItemCount = 5             // 骨架屏显示数量
 *        onRefresh = { /* 刷新逻辑 */ }    // 刷新回调
 *        onLoadMore = { page -> /* 加载更多逻辑 */ }  // 加载更多回调
 *        onRetry = { /* 重试逻辑 */ }      // 重试回调
 *        onNoMore = { /* 没有更多数据的处理 */ }
 *    }
 *
 * ========== 手动控制状态（不推荐，特殊情况使用）==========
 *
 *    binding.listWidget.showLoading()      // 显示加载状态
 *    binding.listWidget.showSkeleton()     // 显示骨架屏
 *    binding.listWidget.showEmpty()        // 显示空数据
 *    binding.listWidget.showError()        // 显示错误
 *    binding.listWidget.showListContent()  // 显示列表内容
 *    binding.listWidget.setPage(2)         // 设置页码
 *    binding.listWidget.setHasMore(false)  // 设置是否还有更多
 *    binding.listWidget.finishRefresh()    // 结束刷新
 *    binding.listWidget.finishLoadMore()   // 结束加载更多
 *
 * ========== 局部刷新功能（高性能）==========
 *
 * 刷新单个 item：
 *    binding.listWidget.refreshItem(0)                    // 刷新第一个 item
 *    binding.listWidget.refreshItem(userModel)            // 根据数据刷新（自动查找位置）
 *
 * 局部刷新（使用 Payload，只更新部分视图）：
 *    binding.listWidget.refreshItem(0, "update_like")     // 只更新点赞状态
 *
 * 在 setup 中处理 Payload：
 *    binding.listWidget.setup {
 *        addType<UserModel>(R.layout.item_user)
 *        onBind {
 *            val model = getModel<UserModel>()
 *            val itemBinding = ItemUserBinding.bind(itemView)
 *
 *            // 处理局部刷新的 payload
 *            if (payload.isNotEmpty()) {
 *                when (payload[0]) {
 *                    "update_like" -> itemBinding.btnLike.isChecked = model.isLiked
 *                    "update_follow" -> itemBinding.btnFollow.text = model.followText
 *                }
 *                return@onBind  // 只更新指定视图，不重新绑定整个 item
 *            }
 *
 *            // 完整绑定（首次加载或无 payload 时）
 *            itemBinding.tvName.text = model.name
 *            itemBinding.tvEmail.text = model.email
 *            itemBinding.btnLike.isChecked = model.isLiked
 *            itemBinding.btnFollow.text = model.followText
 *        }
 *    }
 *
 * 批量刷新：
 *    binding.listWidget.refreshItems(listOf(0, 1, 2))      // 刷新多个位置
 *    binding.listWidget.refreshRange(0, 10)                // 刷新前 10 个
 *
 * 增删改操作：
 *    binding.listWidget.insertItem(0, newUserModel)        // 在开头插入
 *    binding.listWidget.removeItemAt(5)                    // 移除第 5 个
 *    binding.listWidget.removeItem(userModel)              // 移除指定数据
 *    binding.listWidget.moveItem(0, 5)                     // 移动 item
 *
 * 查询操作：
 *    val list = binding.listWidget.getCurrentList()        // 获取当前数据列表
 *    val count = binding.listWidget.getItemCount()         // 获取数据数量
 *
 * ========== 性能优化建议 ==========
 *
 * 1. 优先使用局部刷新（Payload）而不是全量刷新
 * 2. 点赞、关注等操作使用 refreshItem(position, payload)
 * 3. 列表重新排序使用 moveItem 而不是 setModels
 * 4. 插入单条数据使用 insertItem 而不是 addData
 *
 * ========== 多选列表组件 MultiSelectListWidget ==========
 *
 * MultiSelectListWidget 继承自 BaseListWidget，增加了多选功能。
 *
 * 在布局中使用：
 *    <com.mvi.ui.widget.brvlayout.MultiSelectListWidget
 *        android:id="@+id/multiSelectListWidget"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent" />
 *
 * 基础用法：
 *    // 配置组件
 *    binding.multiSelectListWidget.configure {
 *        enableRefresh = true
 *        enableLoadMore = true
 *        onRefresh = { viewModel.loadData(1) }
 *        onLoadMore = { page -> viewModel.loadData(page) }
 *    }
 *
 *    // 设置选择模式
 *    binding.multiSelectListWidget.setSelectionMode(
 *        MultiSelectListWidget.SelectionMode.MULTIPLE
 *    )
 *
 *    // 配置列表项（带多选支持）
 *    binding.multiSelectListWidget.setupWithSelection(
 *        itemResId = R.layout.item_user,
 *        onBind = { model, isSelected ->
 *            val itemBinding = ItemUserBinding.bind(itemView)
 *            itemBinding.tvName.text = model.name
 *            itemBinding.checkBox.isChecked = isSelected  // 更新选中状态
 *
 *            // 设置点击事件（点击切换选中状态）
 *            itemBinding.root.setOnClickListener {
 *                val position = adapterPosition
 *                binding.multiSelectListWidget.toggleSelection(position)
 *                binding.multiSelectListWidget.refreshItem(position)
 *            }
 *        }
 *    )
 *
 *    // 绑定数据
 *    binding.multiSelectListWidget.bindData(this, viewModel.uiState)
 *
 *    // 监听选中变化
 *    binding.multiSelectListWidget.onSelectionChanged = { items, positions ->
 *        toast("已选中 ${items.size} 项")
 *    }
 *
 * 多选操作：
 *    binding.multiSelectListWidget.selectAll()              // 全选
 *    binding.multiSelectListWidget.invertSelection()        // 反选
 *    binding.multiSelectListWidget.clearSelection()         // 清空选择
 *    binding.multiSelectListWidget.exitSelectionMode()      // 退出选择模式
 *
 * 获取选中项：
 *    val selectedItems = binding.multiSelectListWidget.getSelectedItems()
 *    val selectedPositions = binding.multiSelectListWidget.getSelectedPositions()
 *    val count = binding.multiSelectListWidget.getSelectedCount()
 *
 * 长按进入选择模式：
 *    binding.multiSelectListWidget.setLongPressToSelect(true)
 *    binding.multiSelectListWidget.onEnterSelectionMode = {
 *        toast("进入选择模式")
 *    }
 *
 * 单选模式：
 *    binding.multiSelectListWidget.setSelectionMode(
 *        MultiSelectListWidget.SelectionMode.SINGLE
 *    )
 *
 * 批量删除示例：
 *    binding.btnBatchDelete.setOnClickListener {
 *        val selectedItems = binding.multiSelectListWidget.getSelectedItems()
 *        viewModel.batchDelete(selectedItems)
 *        binding.multiSelectListWidget.exitSelectionMode()
 *    }
 *
 * ========== 内存泄漏防护 ==========
 *
 * BaseListWidget 已内置内存泄漏防护机制：
 *
 * 1. **自动清理**：View 从窗口分离时自动调用 release()
 *    ```kotlin
 *    // 不需要手动调用，系统会自动处理
 *    override fun onDetachedFromWindow() {
 *        super.onDetachedFromWindow()
 *        release()  // 自动释放资源
 *    }
 *    ```
 *
 * 2. **协程管理**：自动取消协程，防止泄漏
 *    ```kotlin
 *    bindDataJob?.cancel()  // 自动取消旧的协程
 *    ```
 *
 * 3. **自动防止重复绑定**：多次调用 bindData 会自动取消之前的协程
 *    ```kotlin
 *    binding.listWidget.bindData(this, viewModel.uiState)  // 第一次绑定
 *    binding.listWidget.bindData(this, viewModel.uiState)  // 自动取消第一次，防止泄漏
 *    ```
 *
 * 4. **特殊情况手动释放**：Fragment 快速切换时可以手动调用
 *    ```kotlin
 *    override fun onDestroyView() {
 *        super.onDestroyView()
 *        binding.listWidget.release()  // 可选：手动释放
 *    }
 *    ```
 *
 * 5. **避免在回调中持有长生命周期引用**：
 *    ```kotlin
 *    // ❌ 错误：直接引用 Activity
 *    binding.listWidget.bindData(this, viewModel.uiState) {
 *        activity?.finish()  // 可能泄漏 Activity
 *    }
 *
 *    // ✅ 正确：使用弱引用或避免引用
 *    binding.listWidget.bindData(this, viewModel.uiState) {
 *        // 只更新数据，不引用外部对象
 *        notifyDataSetChanged()
 *    }
 *    ```
 */
