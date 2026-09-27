private fun startLoop() {
        if (isRunning) return
        isRunning = true
        scope = CoroutineScope(Dispatchers.Default + Job())
        scope?.launch {
            while (isRunning) {
                currentPid = -1
                for (pkg in supportedPackages) {
                    val pid = MemoryUtils.findProcessId(pkg)
                    if (pid != -1) {
                        currentPid = pid
                        break
                    }
                }
                
                if (currentPid != -1) {
                    val libBase = MemoryUtils.getModuleBase(currentPid, "libUE4.so")
                    
                    if (libBase != 0L) {
                        // قراءة Matrix بدقة وثبات
                        val tempMatrix = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)
                        if (tempMatrix[0] != 0f) {
                            viewMatrix = tempMatrix
                        }
                        
                        // قراءة GWorld مع التحقق من صحة المؤشر لمنع التذبذب (true/false)
                        val gWorldPtr = MemoryUtils.readLong(currentPid, libBase + GWORLD_BASE_OFFSET)
                        
                        var count = 0
                        val tempPlayers = mutableListOf<MemoryUtils.Vector3>()

                        // التحقق من أن عنوان GWorld في النطاق الصحيح للذاكرة
                        if (gWorldPtr != 0L && gWorldPtr > 0x10000000L && gWorldPtr < 0x7FFFFFFFFFFFFFFL) {
                            val persistentLevel = MemoryUtils.readLong(currentPid, gWorldPtr + OFFSET_PERSISTENT_LEVEL)
                            if (persistentLevel != 0L && persistentLevel > 0x10000000L) {
                                val actorsPtr = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_ARRAY)
                                val actorsCount = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_COUNT).toInt()
                                
                                if (actorsPtr != 0L && actorsCount in 1..10000) {
                                    val maxCount = minOf(actorsCount, 800)
                                    for (i in 0 until maxCount) {
                                        val actor = MemoryUtils.readLong(currentPid, actorsPtr + (i * 8L))
                                        if (actor != 0L && actor > 0x10000000L) {
                                            // قراءة الكومبوننت والتأكد من إحداثيات اللاعب الحقيقية
                                            val rootComponent = MemoryUtils.readLong(currentPid, actor + 0x208L)
                                            if (rootComponent != 0L && rootComponent > 0x10000000L) {
                                                val x = MemoryUtils.readFloat(currentPid, rootComponent + 0x1E4L)
                                                val y = MemoryUtils.readFloat(currentPid, rootComponent + 0x1E8L)
                                                val z = MemoryUtils.readFloat(currentPid, rootComponent + 0x1ECL)
                                                
                                                // فلترة الإحداثيات الوهمية لجلب اللاعبين الحقيقيين فقط
                                                if (x != 0f && y != 0f && Math.abs(x) < 500000f && Math.abs(y) < 500000f) {
                                                    tempPlayers.add(MemoryUtils.Vector3(x, y, z))
                                                    count++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        synchronized(playerList) {
                            playerList.clear()
                            playerList.addAll(tempPlayers)
                        }
                        
                        // تثبيت حالة GW: true طالما المؤشر سليماً
                        val isGWValid = (gWorldPtr != 0L && gWorldPtr > 0x10000000L)
                        statusMessage = "PID: $currentPid | GW: $isGWValid | Players: $count"
                    } else {
                        statusMessage = "PID: $currentPid | WAITING FOR LIB..."
                    }
                } else {
                    statusMessage = "WAITING FOR PUBG..."
                }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(15L) // تقليل التأخير لسرعة التحديث
            }
        }
    }
