package me.relex.widget.waveform;

/**
 * Copyright (C), 2021-2099
 *
 * @author Cooper
 * History:
 * author - date - version - desc
 * Cooper 2022/9/29 09:42 1  简述该类的作用等
 */
public interface WaveFormSelectionViewListener {
    void onHandlerSelectionChanged(long startTime, long endTime);

    void onPlayTimeSelectionChanged(long currentTime);
}
