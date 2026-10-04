$path = "c:\KnowToMigrate\apps\windows-wpf\MainWindow.xaml"
$content = Get-Content $path -Raw

$replacement = @"
                    <!-- TAB 4: Migration Ledger (20px Radius, Liquid Glass AMOLED Audit Log) -->
                    <Grid x:Name="ViewHistory" Visibility="Collapsed">
                        <Border Background="#080808" BorderBrush="#1C1C1C" BorderThickness="1" CornerRadius="20" Padding="28">
                            <DockPanel>
                                <!-- Top Header & Action Controls -->
                                <Grid DockPanel.Dock="Top" Margin="0,0,0,20">
                                    <Grid.ColumnDefinitions>
                                        <ColumnDefinition Width="*" />
                                        <ColumnDefinition Width="Auto" />
                                    </Grid.ColumnDefinitions>
                                    <StackPanel Grid.Column="0">
                                        <TextBlock Text="Migration Ledger &amp; Cryptographic Audit Log" Foreground="#FFFFFF" FontWeight="Bold" FontSize="22" />
                                        <TextBlock Text="Permanent on-device record of peer-to-peer transfers, SHA-256 integrity proofs, and Web Share uploads." Foreground="#888888" FontSize="13" Margin="0,4,0,0" />
                                    </StackPanel>
                                    <StackPanel Grid.Column="1" Orientation="Horizontal" VerticalAlignment="Center">
                                        <Button Content="Open Downloads" Click="BtnOpenDownloadsFolder_Click" Style="{StaticResource KmGlassButton}" Height="34" Padding="14,0" FontSize="11" Margin="0,0,8,0" />
                                        <Button Content="Export Audit Log" Click="BtnExportLedger_Click" Style="{StaticResource KmGlassButton}" Height="34" Padding="14,0" FontSize="11" Margin="0,0,8,0" />
                                        <Button Content="Clear Ledger" Click="BtnClearLedger_Click" Style="{StaticResource KmGlassButton}" Height="34" Padding="14,0" FontSize="11" />
                                    </StackPanel>
                                </Grid>

                                <!-- Empty State Card (When 0 records exist) -->
                                <Border x:Name="PanelLedgerEmpty" Background="#0C0C0C" BorderBrush="#1C1C1C" BorderThickness="1" CornerRadius="16" Padding="40,48" VerticalAlignment="Center" HorizontalAlignment="Center" MaxWidth="500" Visibility="Collapsed">
                                    <StackPanel HorizontalAlignment="Center">
                                        <Border Width="60" Height="60" CornerRadius="30" Background="#140D00" BorderBrush="#FF5A00" BorderThickness="1.5" HorizontalAlignment="Center" Margin="0,0,0,16">
                                            <Path Data="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" Fill="#FF8A00" Width="26" Height="26" Stretch="Uniform" HorizontalAlignment="Center" VerticalAlignment="Center" />
                                        </Border>
                                        <TextBlock Text="No Migration Records Yet" Foreground="#FFFFFF" FontWeight="Bold" FontSize="18" HorizontalAlignment="Center" Margin="0,0,0,6" />
                                        <TextBlock Text="Incoming and outgoing peer transfers, folder migrations, and Web Share downloads will be permanently logged here with cryptographic SHA-256 verification." Foreground="#777777" FontSize="12" TextWrapping="Wrap" TextAlignment="Center" LineHeight="18" />
                                    </StackPanel>
                                </Border>

                                <!-- List of Historical Transfers -->
                                <ListBox x:Name="ListHistory" Background="Transparent" BorderThickness="0" ScrollViewer.HorizontalScrollBarVisibility="Disabled">
                                    <ListBox.ItemTemplate>
                                        <DataTemplate>
                                            <Border Margin="0,0,0,8" Background="#0E0E0E" BorderBrush="#1C1C1C" BorderThickness="1" CornerRadius="14" Padding="16,14">
                                                <Grid>
                                                    <Grid.ColumnDefinitions>
                                                        <ColumnDefinition Width="Auto" />
                                                        <ColumnDefinition Width="*" />
                                                        <ColumnDefinition Width="Auto" />
                                                    </Grid.ColumnDefinitions>

                                                    <!-- Direction Badge -->
                                                    <Border Grid.Column="0" Background="{Binding DirectionBgBrush}" BorderBrush="{Binding DirectionBrush}" BorderThickness="1" CornerRadius="8" Padding="10,4" Margin="0,0,14,0" VerticalAlignment="Center">
                                                        <TextBlock Text="{Binding DirectionDisplay}" Foreground="{Binding DirectionBrush}" FontWeight="Bold" FontSize="11" />
                                                    </Border>

                                                    <!-- Details -->
                                                    <StackPanel Grid.Column="1" VerticalAlignment="Center">
                                                        <TextBlock Text="{Binding FileName}" Foreground="#FFFFFF" FontWeight="SemiBold" FontSize="13" TextTrimming="CharacterEllipsis" />
                                                        <StackPanel Orientation="Horizontal" Margin="0,4,0,0">
                                                            <TextBlock Text="{Binding FormattedSize}" Foreground="#FF8A00" FontSize="11" FontWeight="Bold" />
                                                            <TextBlock Text=" · " Foreground="#555555" FontSize="11" />
                                                            <TextBlock Text="{Binding DeviceName}" Foreground="#AAAAAA" FontSize="11" />
                                                            <TextBlock Text=" · " Foreground="#555555" FontSize="11" />
                                                            <TextBlock Text="{Binding Transport}" Foreground="#777777" FontSize="11" />
                                                            <TextBlock Text=" · " Foreground="#555555" FontSize="11" />
                                                            <TextBlock Text="{Binding FormattedTime}" Foreground="#666666" FontSize="11" />
                                                        </StackPanel>
                                                    </StackPanel>

                                                    <!-- Verification Status Badge & Open Button -->
                                                    <StackPanel Grid.Column="2" Orientation="Horizontal" VerticalAlignment="Center">
                                                        <Border Background="#0D2614" BorderBrush="#22C55E" BorderThickness="1" CornerRadius="8" Padding="8,4" Margin="0,0,8,0">
                                                            <TextBlock Text="{Binding VerifiedBadge}" Foreground="#22C55E" FontSize="10" FontWeight="Bold" />
                                                        </Border>
                                                        <Button Content="Show in Folder" Tag="{Binding FilePath}" Click="BtnShowHistoryFile_Click" Style="{StaticResource KmGlassButton}" Height="30" Padding="10,0" FontSize="11" />
                                                    </StackPanel>
                                                </Grid>
                                            </Border>
                                        </DataTemplate>
                                    </ListBox.ItemTemplate>
                                </ListBox>
                            </DockPanel>
                        </Border>
                    </Grid>
"@

$regex = '(?s)<!-- TAB 4: History.*?<!-- TAB 5: Security Center'
$newContent = [regex]::Replace($content, $regex, "$replacement`r`n`r`n                    <!-- TAB 5: Security Center")
Set-Content -Path $path -Value $newContent -Encoding utf8
Write-Host "Updated MainWindow.xaml ViewHistory section successfully."
