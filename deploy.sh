#!/bin/bash
cd /root/crm/microelCRM
git pull
yes | cp /root/crm/microelCRM/crm.service /etc/systemd/system/crm.service
systemctl daemon-reload
systemctl enable crm
systemctl restart crm
