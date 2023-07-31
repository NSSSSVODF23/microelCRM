#!/usr/bin/php
<?php 
//Этой файл предназначен для выполнения различных команд cервера как демон.
//Выполнение команд происходит через вызов сервера по RPC. 
//Если не включен явно режим консоли (-d) - весь вывод пишем только в лог -l file
//Понимает хелпу (при -h -d включается автоматически)
//Имя команды в виде -r group.cmd, остальные-аргументы к ней в виде arg idx=arg
//которые интерпретируются внутри выполнения команды на сервере.
//Может вызыватся из любого места, ни к чему не привязан, но логи пишет в свою папку
//Параметры RPC содержатся в исходнике

//базовая конфигурация демона. Править при установке. Более ничего править не надо!
$RPC="rpc://10.50.0.7:91/rpc_server.php;daemons.WDaemon2;sandbox:130";
$SelfName='daemon_x';  //имя демона, на сервере для него д.б. конфиг
$PHPFname='WDaemon.php';  //имя своего файла - нужен для автоопределения пути
//$Def_ip='10.50.0.7'

//Базовая хелпа для запуска файла без параметров
$BaseHelp="Рабочий запуск -r group.cmd arg idx=arg ... 
[-d - не пишем в консоль (типа daemon)],
[-D - На консоль массив ответа rpc (детальная отладка)],
[-A - на консоль получить 1 строку - serialize масив ответа (для PHP system)]
[-l fname - ответ пишем в файл в режиме a]
Для получения хелпы: 
-h - выдаст спиоск команд (если реализовано на сервере)
-h cmd - подробное описание команды (если реализовано на сервере)
Разрешенные команды и их описания находятся в конфигах сервера
idx=arg... - дополнительные аргументы, получим в обработчике как rq\n";


ini_set('error_reporting',E_ALL|E_STRICT);
ini_set('display_errors','on');
global $cfg;  global $arg; global $Cons; global $FLog; global $Path; 
  $Cons=1; $FLog=0; $Path=''; $Debug=0;
  $Path=str_replace($PHPFname,'',$argv[0]); 
  $acnt=count($argv);
  if($acnt==1) return DRet($BaseHelp);
  $rc=parceArgs($argv,$arg,$cfg);  if($rc) return DRet($rc); //выделим параметры с - и простые аргументы
  if(isset($cfg['help'])) $arg['help']=$cfg['help']; //здесь хотим хелпу, остальное игнор
  else {  //ожидаем команду на выполнение
    if(isset($cfg['log'])) $FLog=$cfg['log']; 
    if(!isset($cfg['cmd'])) return DRet("Не указана команда для выполнения");
    $arg['__call']=$cfg['cmd']; }
  $ssh=getenv('SSH_CONNECTION');  //разберемся с авторизацией терминала
  $ucur=getenv('USER');  //это имя текущего юзера
//  $ucur=get_current_user(); 
  if($ssh) { $a1=explode(' ',$ssh); if(isset($a1[2])) $ip=$a1[2]; else $ip=$a[2];
    $arg['__ip']=$ip; $arg['__person']=$ucur.':'.$ip; }
  else $ssh='cons'; 
//  $arg['__src']=$ucur.':'.$ssh; 
//  $auth=array('dname'=>$SelfName,'ip'=>$ip); //ключ для авторизации по конфигу сервера
  $rc=Run_rpc($RPC,$SelfName,$arg);
  return DRet($rc);


function DRet($msg) { 
//Корректно завершить демона. Если $Cons активен - делаем echo
//Если есть $FLog - пишем и в лог msg; Возвращаем всегда 0.
  global $Cons; global $FLog; global $Path; global $Debug;
  if($Cons==1 || $Cons==2) 
    if(is_string($msg)) echo $msg."\n"; 
    else { $rc=var_export($msg); $msg1=str_replace("\n",' ',$rc); echo $msg1."\n"; }
  if($Cons==3) echo serialize($msg); 
  if($FLog) { if($FLog{0}=='/') $fp=$FLog; else $fp=$Path.'/'.$FLog;
    $dt=date('ymd-His');
    $fd=fopen($fp,'a'); if(!$fd) return 5;
    if(!is_string($msg)) { $rc=var_export($msg,true); $msg2=str_replace("\n",' ',$rc); }
    else $msg2=$msg;
    $s="$dt: $msg2\n"; fputs($fd,$s); fclose($fd); }
  return 0; }

function parceArgs($argv,&$arg,&$cfg) {
//разобрать строку аргументов и заполнить массивы.
  $pneed=false; global $Cons; global $Debug;
  foreach($argv as $str) { 
    if($pneed) { $cfg[$pneed]=$str; $pneed=false; continue; } 
    if($str=='-h') { $pneed='help'; $Cons=1; $cfg['help']=''; continue; }
    if($str=='-c') { $pneed='conf'; continue; }
//    if($str=='-i') { $pneed='info'; $Cons=1; $cfg['info']=''; continue; }
    if($str=='-d') { $Cons=0; continue; }
    if($str=='-D') { $Cons=2; continue; }
    if($str=='-A') { $Cons=3; continue; }
    if($str=='-l') { $pneed='log'; continue; }
    if($str=='-r') { $pneed='cmd'; continue; }
    if($pos=strpos($str,'=')) $arg[substr($str,0,$pos)]=substr($str,$pos+1);
    else $arg[]=$str; }
  return; }

function Run_rpc($srv,$dname,$args) {
//Обертка для _call_server, адаптированная под текущий проект. 
//Получает $srv из конфига, auth=cfg,$args, и формирует из них правилные 
//параметры (с учетом особенностей проекта) для _call_server и вызывает его.
  $perm=0x0ffff;
  $cfg=explode(';',$srv); if(count($cfg)<2) return "-Run_rpc_bad_conf_format_($srv)";
  if($args) trim_array($args,1);  //рекурсивно очистить пробелы (косяк либы RPC)
  $pos=strpos($cfg[1],'.'); $method=substr($cfg[1],$pos+1);
  $cfg[1]=substr($cfg[1],0,$pos).'.*';  //тут особенности вызова сервера
  if(!isset($cfg[2])) $cfg[2]=false; //сигнатуры серверу не нужны
    print_r($cfg);
    print_r($method);
    print_r($args);
    print_r($dname);
  $ans=_call_server($cfg[0],$cfg[1],$method,$args,$dname,$cfg[2]);
  if(!is_array($ans)) return $ans; //суровая ошибка на сервере
//  if(!isset($ans['state'])) return "-RPC_bad_format_answer";
  if(!isset($ans['state'])) return $ans;  //трап на сервере, получили строку ругани консоли, выдаем ее
  global $Cons; if($Cons>1) return $ans;  //режим -D | -A - выдаем массив без коментариев, высший уровень будет с ним разбиратся
  if(isset($ans['__msg'])) $msg=$ans['__msg']; else $msg=false;
  if($msg) return $msg;  //если есть сообщение (одна строка)мону - выдаем только его без интерпретации
  if($ans['state']=='OK') return '+OK';  //Сообщения нет, просто говорим что все ОК
  return "-Error: state=".$ans['state']; //Сообщения нет, но ошибка от сервера
  return $ans; 
}

function _call_server($srv,$class,$method,$args,$dname,$sign=false) {
//выполняет собственно вызов сервера. Принимает кучу всяких параметров.
//выдает или массив от сервера или строку с ошибкой.
//srv-формат rpc://ip:port, class-class.*-имя класса шлюза, method-метод шлюза
//если нужна (сервер требует) сигнатура - sign='host:key'
//_Sys-массив auth (login,name,perm,ip) - должен быть установлен на входе или внутри
//заточен под сервер Bond_a, модифицированный. 
  global $Path;
  include_once $Path."\IXR_Library.php";
  $srv=str_replace('rpc://','http://',$srv);
  if(!$args) $args=array(); //таки нужен массив, пусть и пустой
  $dat=array(); $dat['method']=$method; //так хочет сервер
  if(is_array($args)) $dat['rq']=$args; else  $dat['rq']=unserialize($args); 
  if(!$dname) return "-Err: Не задано имя демона!";
  $dat['_Sys']=array('auth'=>array('dname'=>$dname));
  if($sign) { $sg=explode(':',$sign);
    $s=serialize($dat['rq']).$sg[1];
    $dat['__hostname']=$sg[0]; $dat['__sign']=crc32($s); }
  $call=new IXR_Client($srv);  //только создали класс для коннекта
  $call->debug = -1; //режим отлова нештатных ситуаций (включая трапы сервера)
    print_r($s);
    print_r($dat);
  $rc=$call->query($class,$dat);
  if(!$rc)  //произошла ошибка как правило с конектом или сетью
    return "-ERR_connect_error: (".$call->getErrorMessage().")\n";
  if(is_string($rc)) { //серьезная проблема на сервере (возможно синтаксис или трап)
    $ret="-ERR_server_error: \n".$rc;
    return $ret; }
  $ans=$call->getResponse();
  if(!is_array($ans)) return "-ERR_bad_answer: (".$ans.")\n";
  if(!isset($ans['state'])) $ans['state']="-NOT_SET";  //пометим что косяк, но иак быть не должно
  unset($ans['_Sys']); //нефиг кишки показывать
  return $ans; }

function trim_array(array &$a,$rec=false) {
//сделать trim каждому элементу массива
  foreach($a as $i=>$v) 
    if($rec && is_array($v)) { trim_array($v,$rec); $a[$i]=$v; }
    else if(is_string($v) && $v) $a[$i]=trim($v);
  return; }
?>
